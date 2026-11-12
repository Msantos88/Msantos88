package br.com.barbearia.adapters.payment;

import br.com.barbearia.application.GatewayPagamento.CobrancaPix;
import br.com.barbearia.application.GatewayPagamento.NovaCobranca;
import br.com.barbearia.application.GatewayPagamento.StatusPagamento;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class MercadoPagoGatewayTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final MercadoPagoGateway gateway = new MercadoPagoGateway(
            new MercadoPagoProperties("https://api.mercadopago.com", "TEST-token", null,
                    "https://minhabarbearia.com.br/api/pagamentos/webhook/mercadopago"), builder);

    @Test
    void criaCobrancaPixComValorDoSinal() {
        servidor.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer TEST-token"))
                .andExpect(header("X-Idempotency-Key", "ag-1"))
                .andExpect(jsonPath("$.transaction_amount").value(25.00))
                .andExpect(jsonPath("$.payment_method_id").value("pix"))
                .andExpect(jsonPath("$.external_reference").value("ag-1"))
                .andExpect(jsonPath("$.date_of_expiration").value("2026-11-17T09:30:00.000-03:00"))
                .andExpect(jsonPath("$.payer.email").value("cliente@exemplo.com"))
                .andRespond(withSuccess("""
                        {"id": 987, "status": "pending",
                         "point_of_interaction": {"transaction_data": {
                           "qr_code": "000201PIX", "qr_code_base64": "iVBOR", "ticket_url": "https://mp/ticket"}}}
                        """, MediaType.APPLICATION_JSON));

        CobrancaPix c = gateway.criarCobrancaPix(new NovaCobranca("ag-1", new BigDecimal("25.00"), "Sinal",
                "Cliente", "cliente@exemplo.com",
                OffsetDateTime.of(2026, 11, 17, 9, 30, 0, 0, ZoneOffset.ofHours(-3))));

        assertThat(c.pagamentoId()).isEqualTo("987");
        assertThat(c.copiaECola()).isEqualTo("000201PIX");
        assertThat(c.qrCodeBase64()).isEqualTo("iVBOR");
        servidor.verify();
    }

    @Test
    void traduzStatusDoPagamento() {
        servidor.expect(requestTo("https://api.mercadopago.com/v1/payments/987"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"id": 987, "status": "approved", "transaction_amount": 25.0, "external_reference": "ag-1"}
                        """, MediaType.APPLICATION_JSON));

        var p = gateway.consultar("987");

        assertThat(p.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(p.valor()).isEqualByComparingTo("25.00");
        assertThat(p.referencia()).isEqualTo("ag-1");
    }
}
