package br.com.barbearia;

import br.com.barbearia.adapters.payment.SimuladorGateway;
import br.com.barbearia.application.AgendamentoRepository;
import br.com.barbearia.application.AgendamentoService;
import br.com.barbearia.domain.StatusAgendamento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgendamentoFluxoTest {

    /** Terça-feira, 17/11/2026, 08:00 em São Paulo. */
    static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 17, 8, 0);
    static final Long CORTE = 1L;          // R$ 50,00 / 30 min (seed)
    static final Long CORTE_E_BARBA = 3L;  // R$ 80,00 / 60 min
    static final Long JOAO = 1L;
    static final Long PEDRO = 2L;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste(AGORA, ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RelogioDeTeste relogio;
    @Autowired AgendamentoRepository repositorio;
    @Autowired AgendamentoService service;
    @Autowired SimuladorGateway simulador;

    @BeforeEach
    void limpar() {
        repositorio.deleteAll();
        relogio.definir(AGORA);
    }

    @Test
    void reservaCobraSinalDeCinquentaPorCentoEBloqueiaOHorario() throws Exception {
        JsonNode ag = reservar(CORTE, JOAO, "2026-11-17T10:00:00");

        assertThat(ag.get("status").asText()).isEqualTo("AGUARDANDO_PAGAMENTO");
        assertThat(ag.get("valorTotal").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(ag.get("valorSinal").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(ag.get("valorRestante").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(ag.at("/pix/copiaECola").asText()).isNotBlank();
        assertThat(ag.get("pagarAte").asText()).isEqualTo("2026-11-17T08:30:00-03:00");

        // horário some da lista para o mesmo barbeiro, mas continua livre para outro
        assertThat(horarios(JOAO, CORTE, "2026-11-17")).doesNotContain("10:00:00");
        assertThat(horarios(PEDRO, CORTE, "2026-11-17")).contains("10:00:00");

        // ninguém mais consegue reservar o mesmo horário (nem sobrepondo)
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(CORTE_E_BARBA, JOAO, "2026-11-17T09:30:00")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void pagamentoDoSinalConfirmaAReserva() throws Exception {
        String id = reservar(CORTE, JOAO, "2026-11-17T10:00:00").get("id").asText();

        mvc.perform(post("/api/simulador/agendamentos/" + id + "/pagar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.pix").doesNotExist());

        // passar do prazo depois de pago não libera o horário
        relogio.avancar(Duration.ofHours(1));
        service.verificarPendentes();
        assertThat(statusDe(id)).isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(horarios(JOAO, CORTE, "2026-11-17")).doesNotContain("10:00:00");
    }

    @Test
    void semPagamentoNoPrazoAReservaExpiraEOHorarioVoltaASerLivre() throws Exception {
        String id = reservar(CORTE, JOAO, "2026-11-17T11:00:00").get("id").asText();

        relogio.avancar(Duration.ofMinutes(29));
        service.verificarPendentes();
        assertThat(statusDe(id)).isEqualTo(StatusAgendamento.AGUARDANDO_PAGAMENTO);

        relogio.avancar(Duration.ofMinutes(2));
        // mesmo antes do job rodar, o horário já aparece como livre
        assertThat(horarios(JOAO, CORTE, "2026-11-17")).contains("11:00:00");
        service.verificarPendentes();
        assertThat(statusDe(id)).isEqualTo(StatusAgendamento.EXPIRADO);
    }

    @Test
    void pagamentoAtrasadoComHorarioLivreAindaConfirma() throws Exception {
        String id = reservar(CORTE, JOAO, "2026-11-17T12:00:00").get("id").asText();
        relogio.avancar(Duration.ofMinutes(40));
        service.verificarPendentes();
        assertThat(statusDe(id)).isEqualTo(StatusAgendamento.EXPIRADO);

        pagar(id);

        assertThat(statusDe(id)).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void pagamentoAtrasadoComHorarioJaOcupadoViraReembolso() throws Exception {
        String primeiro = reservar(CORTE, JOAO, "2026-11-17T12:00:00").get("id").asText();
        relogio.avancar(Duration.ofMinutes(40));
        String segundo = reservar(CORTE, JOAO, "2026-11-17T12:00:00").get("id").asText();

        pagar(primeiro);

        assertThat(statusDe(primeiro)).isEqualTo(StatusAgendamento.REEMBOLSO_PENDENTE);
        assertThat(statusDe(segundo)).isEqualTo(StatusAgendamento.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void respeitaExpedienteDiasDeFuncionamentoEAntecedencia() throws Exception {
        // segunda-feira: fechado
        assertThat(horarios(JOAO, CORTE, "2026-11-16")).isEmpty();
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(CORTE, JOAO, "2026-11-23T10:00:00")))
                .andExpect(status().isUnprocessableEntity());
        // às 08:00, com 60 min de antecedência, o primeiro horário é 09:00; o último corte termina às 19:00
        JsonNode hoje = json.readTree(mvc.perform(get("/api/horarios")
                .param("barbeiroId", "1").param("servicoId", "1").param("data", "2026-11-17"))
                .andReturn().getResponse().getContentAsString());
        assertThat(hoje.get(0).asText()).isEqualTo("09:00:00");
        assertThat(hoje.get(hoje.size() - 1).asText()).isEqualTo("18:30:00");
        // fora do expediente / fora da grade
        for (String inicio : new String[]{"2026-11-17T18:45:00", "2026-11-17T10:10:00", "2026-11-17T08:30:00"}) {
            mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                            .content(corpo(CORTE, JOAO, inicio)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Test
    void validaDadosDoCliente() throws Exception {
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servicoId":1,"barbeiroId":1,"inicio":"2026-11-17T10:00:00",
                                 "clienteNome":"","clienteTelefone":"abc","clienteEmail":"x"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void painelAdminExigeLoginECsrf() throws Exception {
        reservar(CORTE, JOAO, "2026-11-17T10:00:00");

        mvc.perform(get("/api/admin/agendamentos").param("data", "2026-11-17"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/agendamentos").param("data", "2026-11-17")
                        .with(httpBasic("admin", "errada")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/agendamentos").param("data", "2026-11-17")
                        .with(httpBasic("admin", "senha-de-teste-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clienteTelefone").value("(11) 91234-5678"));

        String id = repositorio.findAll().get(0).getId().toString();
        mvc.perform(post("/api/admin/agendamentos/" + id + "/cancelar")
                        .with(httpBasic("admin", "senha-de-teste-123")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/agendamentos/" + id + "/cancelar")
                        .with(httpBasic("admin", "senha-de-teste-123")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
        assertThat(horarios(JOAO, CORTE, "2026-11-17")).contains("10:00:00");
    }

    @Test
    void consultaPublicaNaoExpoeDadosDeContato() throws Exception {
        String id = reservar(CORTE, JOAO, "2026-11-17T10:00:00").get("id").asText();
        mvc.perform(get("/api/agendamentos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteTelefone").doesNotExist())
                .andExpect(jsonPath("$.clienteEmail").doesNotExist());
        mvc.perform(get("/api/agendamentos/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- auxiliares

    private JsonNode reservar(Long servico, Long barbeiro, String inicio) throws Exception {
        String resposta = mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(servico, barbeiro, inicio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta);
    }

    private String corpo(Long servico, Long barbeiro, String inicio) {
        return """
                {"servicoId":%d,"barbeiroId":%d,"inicio":"%s",
                 "clienteNome":"Carlos Silva","clienteTelefone":"(11) 91234-5678","clienteEmail":"carlos@exemplo.com"}
                """.formatted(servico, barbeiro, inicio);
    }

    private java.util.List<String> horarios(Long barbeiro, Long servico, String data) throws Exception {
        String r = mvc.perform(get("/api/horarios").param("barbeiroId", barbeiro.toString())
                        .param("servicoId", servico.toString()).param("data", data))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return java.util.Arrays.asList(json.readValue(r, String[].class));
    }

    private void pagar(String id) {
        String pagamentoId = repositorio.findById(UUID.fromString(id)).orElseThrow().getPagamentoId();
        simulador.aprovar(pagamentoId);
        service.processarPagamento(pagamentoId);
    }

    private StatusAgendamento statusDe(String id) {
        return repositorio.findById(UUID.fromString(id)).orElseThrow().getStatus();
    }
}
