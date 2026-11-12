package br.com.barbearia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/**
 * Proteções contra uso abusivo, por IP:
 * <ul>
 *   <li>limite geral de requisições à API;</li>
 *   <li>limite de reservas criadas (impede que um robô segure todos os horários);</li>
 *   <li>bloqueio temporário após muitas senhas erradas no painel.</li>
 * </ul>
 */
class ProtecaoContraAbusoFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ProtecaoContraAbusoFilter.class);

    private final LimitadorDeTaxa api;
    private final LimitadorDeTaxa reservas;
    private final LimitadorDeTaxa loginsErrados;
    private final int tentativasDeLogin;

    ProtecaoContraAbusoFilter(SegurancaProperties props, Clock clock) {
        this.api = new LimitadorDeTaxa(props.requisicoesPorMinuto(), Duration.ofMinutes(1), clock);
        this.reservas = new LimitadorDeTaxa(props.reservasPorIp(), props.janelaReservas(), clock);
        this.loginsErrados = new LimitadorDeTaxa(props.tentativasDeLogin(), props.bloqueioLogin(), clock);
        this.tentativasDeLogin = props.tentativasDeLogin();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ip = req.getRemoteAddr();
        String uri = req.getRequestURI();

        if (exigeLogin(uri)) {
            if (loginsErrados.bloqueado(ip)) {
                recusar(res, loginsErrados.segundosParaLiberar(ip),
                        "Muitas tentativas de login. Tente novamente mais tarde.");
                return;
            }
            boolean enviouSenha = req.getHeader(HttpHeaders.AUTHORIZATION) != null;
            chain.doFilter(req, res);
            if (enviouSenha && res.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
                if (loginsErrados.registrar(ip) >= tentativasDeLogin) {
                    log.warn("IP {} bloqueado no painel após muitas senhas erradas", ip);
                }
            } else if (enviouSenha && res.getStatus() < 400) {
                loginsErrados.zerar(ip);
            }
            return;
        }

        if (uri.startsWith("/api/") && !uri.startsWith("/api/pagamentos/webhook/") && !api.permitir(ip)) {
            recusar(res, api.segundosParaLiberar(ip), "Muitas requisições. Aguarde um instante.");
            return;
        }
        if ("POST".equals(req.getMethod()) && "/api/agendamentos".equals(uri) && !reservas.permitir(ip)) {
            log.warn("IP {} atingiu o limite de reservas", ip);
            recusar(res, reservas.segundosParaLiberar(ip),
                    "Muitas reservas feitas a partir desta conexão. Tente mais tarde ou fale com a barbearia.");
            return;
        }
        chain.doFilter(req, res);
    }

    static boolean exigeLogin(String uri) {
        return uri.startsWith("/api/admin/") || uri.startsWith("/api/simulador/")
                || uri.equals("/admin.html") || uri.equals("/admin.js");
    }

    private static void recusar(HttpServletResponse res, long segundos, String mensagem) throws IOException {
        res.setStatus(429);
        res.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"mensagem\":\"" + mensagem + "\"}");
    }
}
