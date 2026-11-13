package br.com.barbearia;

import br.com.barbearia.application.AgendamentoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.ZoneId;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CadastroDeBarbeirosTest {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste(AgendamentoFluxoTest.AGORA, ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AgendamentoRepository agendamentos;

    @Test
    void novoBarbeiroApareceParaOClienteEPodeSerAgendado() throws Exception {
        mvc.perform(get("/api/barbeiros")).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Ewerton"));

        long id = cadastrar("  Lucas   Silva ");

        mvc.perform(get("/api/barbeiros")).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].nome").value("Lucas Silva"));
        mvc.perform(reserva(id, "2026-11-17T10:00:00")).andExpect(status().isCreated());
    }

    @Test
    void naoPermiteNomeRepetidoOuVazio() throws Exception {
        mvc.perform(admin(post("/api/admin/barbeiros")).content("{\"nome\":\"ewerton\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("Já existe")));
        mvc.perform(admin(post("/api/admin/barbeiros")).content("{\"nome\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desativadoSomeDaPaginaENaoRecebeReservasMasPodeVoltar() throws Exception {
        long id = cadastrar("Lucas");
        mvc.perform(admin(post("/api/admin/barbeiros/" + id + "/desativar")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));

        mvc.perform(get("/api/barbeiros")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(reserva(id, "2026-11-17T10:00:00")).andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/admin/barbeiros"))).andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(admin(post("/api/admin/barbeiros/" + id + "/ativar"))).andExpect(jsonPath("$.ativo").value(true));
        mvc.perform(reserva(id, "2026-11-17T10:00:00")).andExpect(status().isCreated());
    }

    @Test
    void naoDesativaQuemTemClienteAgendadoNemOUltimoBarbeiro() throws Exception {
        long lucas = cadastrar("Lucas");
        mvc.perform(reserva(lucas, "2026-11-17T10:00:00")).andExpect(status().isCreated());

        mvc.perform(admin(post("/api/admin/barbeiros/" + lucas + "/desativar")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("1 agendamento(s) futuro(s)")));

        // Ewerton pode sair, mas aí Lucas vira o único ativo e não pode ser desativado
        mvc.perform(admin(post("/api/admin/barbeiros/1/desativar"))).andExpect(status().isOk());
        agendamentos.deleteAll();
        mvc.perform(admin(post("/api/admin/barbeiros/" + lucas + "/desativar")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("pelo menos um barbeiro ativo")));
    }

    @Test
    void renomeia() throws Exception {
        mvc.perform(admin(put("/api/admin/barbeiros/1")).content("{\"nome\":\"Ewerton Gois\"}"))
                .andExpect(jsonPath("$.nome").value("Ewerton Gois"));
        mvc.perform(get("/api/barbeiros")).andExpect(jsonPath("$[0].nome").value("Ewerton Gois"));
    }

    @Test
    void cadastroExigeLoginECsrf() throws Exception {
        mvc.perform(post("/api/admin/barbeiros").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Invasor\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/barbeiros").with(httpBasic("admin", "senha-de-teste-123"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Invasor\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/barbeiros")).andExpect(jsonPath("$", hasSize(1)));
    }

    private long cadastrar(String nome) throws Exception {
        String r = mvc.perform(admin(post("/api/admin/barbeiros")).content(json.writeValueAsString(
                        java.util.Map.of("nome", nome))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode n = json.readTree(r);
        return n.get("id").asLong();
    }

    private static MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder b) {
        return b.with(httpBasic("admin", "senha-de-teste-123")).with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    private static MockHttpServletRequestBuilder reserva(long barbeiro, String inicio) {
        return post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON).content("""
                {"servicoId":1,"barbeiroId":%d,"inicio":"%s",
                 "clienteNome":"Cliente","clienteTelefone":"1197777%04d","clienteEmail":"c%d@exemplo.com"}
                """.formatted(barbeiro, inicio, barbeiro, barbeiro));
    }
}
