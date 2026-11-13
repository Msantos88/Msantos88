package br.com.barbearia;

import br.com.barbearia.application.GatewayPagamento;
import br.com.barbearia.application.GatewayPagamento.PagamentoConsultado;
import br.com.barbearia.application.GatewayPagamento.StatusPagamento;
import br.com.barbearia.application.AgendamentoRepository;
import br.com.barbearia.application.AgendamentoService;
import br.com.barbearia.application.AgendamentoService.NovoAgendamento;
import br.com.barbearia.domain.Agendamento;
import br.com.barbearia.domain.StatusAgendamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "pagamento.provedor=mercadopago",
        "pagamento.mercadopago.access-token=TEST-x",
        "pagamento.mercadopago.webhook-secret=segredo-webhook"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookMercadoPagoTest {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste(AgendamentoFluxoTest.AGORA, ZoneId.of("America/Sao_Paulo"));
        }
    }

    @MockitoBean GatewayPagamento gateway;
    @Autowired MockMvc mvc;
    @Autowired AgendamentoService service;
    @Autowired AgendamentoRepository repositorio;

    @Test
    void webhookAssinadoConfirmaAposConsultarOMercadoPago() throws Exception {
        when(gateway.criarCobrancaPix(any())).thenReturn(new GatewayPagamento.CobrancaPix("555", "pix", "qr", "url"));
        Agendamento ag = service.reservar(new NovoAgendamento(1L, 1L,
                AgendamentoFluxoTest.AGORA.withHour(15), "Ana", "11912345678", "ana@exemplo.com"));
        when(gateway.consultar("555")).thenReturn(new PagamentoConsultado("555", ag.getId().toString(),
                new BigDecimal("25.00"), StatusPagamento.APROVADO));

        // assinatura inválida: rejeitado sem nem consultar o Mercado Pago
        mvc.perform(post("/api/pagamentos/webhook/mercadopago").param("data.id", "555").param("type", "payment")
                        .header("x-signature", "ts=1,v1=abc").header("x-request-id", "r1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\",\"data\":{\"id\":\"555\"}}"))
                .andExpect(status().isUnauthorized());
        verify(gateway, never()).consultar("555");

        String v1 = br.com.barbearia.adapters.payment.MercadoPagoAssinaturaTestAccess
                .hmac("segredo-webhook", "id:555;request-id:r1;ts:1700000000;");
        mvc.perform(post("/api/pagamentos/webhook/mercadopago").param("data.id", "555").param("type", "payment")
                        .header("x-signature", "ts=1700000000,v1=" + v1).header("x-request-id", "r1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\",\"data\":{\"id\":\"555\"}}"))
                .andExpect(status().isOk());

        assertThat(repositorio.findById(ag.getId()).orElseThrow().getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void vencimentoDoPixRespeitaOMinimoDoMercadoPago() {
        var captor = org.mockito.ArgumentCaptor.forClass(GatewayPagamento.NovaCobranca.class);
        when(gateway.criarCobrancaPix(captor.capture())).thenReturn(new GatewayPagamento.CobrancaPix("888", "pix", "qr", "url"));

        service.reservar(new NovoAgendamento(1L, 1L,
                AgendamentoFluxoTest.AGORA.withHour(17), "Caio", "11977776666", "caio@exemplo.com"));

        // reserva segura o horário por 30 min, mas o PIX vence com folga (>= 30 min exigidos pelo Mercado Pago)
        assertThat(captor.getValue().expiraEm().toLocalDateTime())
                .isEqualTo(AgendamentoFluxoTest.AGORA.plusMinutes(35));
    }

    @Test
    void pagamentoComValorMenorQueOSinalNaoConfirma() {
        when(gateway.criarCobrancaPix(any())).thenReturn(new GatewayPagamento.CobrancaPix("777", "pix", "qr", "url"));
        Agendamento ag = service.reservar(new NovoAgendamento(1L, 1L,
                AgendamentoFluxoTest.AGORA.withHour(16), "Bia", "11988887777", "bia@exemplo.com"));
        when(gateway.consultar("777")).thenReturn(new PagamentoConsultado("777", ag.getId().toString(),
                new BigDecimal("1.00"), StatusPagamento.APROVADO));

        service.processarPagamento("777");

        assertThat(repositorio.findById(ag.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.AGUARDANDO_PAGAMENTO);
    }
}
