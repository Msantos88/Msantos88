package br.com.barbearia;

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.ZoneId;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "barbearia.seguranca.requisicoes-por-minuto=20",
        "barbearia.seguranca.reservas-por-ip=3",
        "barbearia.seguranca.janela-reservas=1h",
        "barbearia.seguranca.tentativas-de-login=5",
        "barbearia.seguranca.bloqueio-login=15m"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProtecaoContraAbusoTest {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste(AgendamentoFluxoTest.AGORA, ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired RelogioDeTeste relogio;

    @Test
    void limitaReservasPorIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(reserva("10.0.0.1", "2026-11-18T1" + i + ":00:00", "1199999000" + i))
                    .andExpect(status().isCreated());
        }
        mvc.perform(reserva("10.0.0.1", "2026-11-18T14:00:00", "11999990009"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // outro IP não é afetado
        mvc.perform(reserva("10.0.0.2", "2026-11-18T14:00:00", "11999990009"))
                .andExpect(status().isCreated());
    }

    @Test
    void bloqueiaPainelAposMuitasSenhasErradas() throws Exception {
        String ip = "10.0.0.50";
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/admin/agendamentos").param("data", "2026-11-17")
                            .with(ip(ip)).with(httpBasic("admin", "chute-" + i)))
                    .andExpect(status().isUnauthorized());
        }
        // agora nem a senha certa entra a partir desse IP
        mvc.perform(get("/api/admin/agendamentos").param("data", "2026-11-17")
                        .with(ip(ip)).with(httpBasic("admin", "senha-de-teste-123")))
                .andExpect(status().isTooManyRequests());
        // de outro IP a barbearia continua entrando
        mvc.perform(get("/api/admin/agendamentos").param("data", "2026-11-17")
                        .with(ip("10.0.0.51")).with(httpBasic("admin", "senha-de-teste-123")))
                .andExpect(status().isOk());
        // passado o bloqueio, libera
        relogio.avancar(Duration.ofMinutes(16));
        mvc.perform(get("/api/admin/agendamentos").param("data", "2026-11-17")
                        .with(ip(ip)).with(httpBasic("admin", "senha-de-teste-123")))
                .andExpect(status().isOk());
    }

    @Test
    void loginCertoZeraContagemDeErros() throws Exception {
        String ip = "10.0.0.60";
        for (int rodada = 0; rodada < 3; rodada++) {
            for (int i = 0; i < 4; i++) {
                mvc.perform(get("/api/admin/agendamentos").param("data", "2026-11-17")
                        .with(ip(ip)).with(httpBasic("admin", "errada"))).andExpect(status().isUnauthorized());
            }
            mvc.perform(get("/api/admin/agendamentos").param("data", "2026-11-17")
                    .with(ip(ip)).with(httpBasic("admin", "senha-de-teste-123"))).andExpect(status().isOk());
        }
    }

    @Test
    void limitaRequisicoesGeraisPorIp() throws Exception {
        String ip = "10.0.0.70";
        for (int i = 0; i < 20; i++) {
            mvc.perform(get("/api/servicos").with(ip(ip))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/servicos").with(ip(ip))).andExpect(status().isTooManyRequests());
        relogio.avancar(Duration.ofMinutes(1));
        mvc.perform(get("/api/servicos").with(ip(ip))).andExpect(status().isOk());
    }

    private static RequestPostProcessor ip(String ip) {
        return req -> {
            req.setRemoteAddr(ip);
            return req;
        };
    }

    private org.springframework.test.web.servlet.RequestBuilder reserva(String ip, String inicio, String telefone) {
        return post("/api/agendamentos").with(ip(ip)).contentType(MediaType.APPLICATION_JSON).content("""
                {"servicoId":1,"barbeiroId":2,"inicio":"%s",
                 "clienteNome":"Robô","clienteTelefone":"%s","clienteEmail":"%s@exemplo.com"}
                """.formatted(inicio, telefone, telefone));
    }
}
