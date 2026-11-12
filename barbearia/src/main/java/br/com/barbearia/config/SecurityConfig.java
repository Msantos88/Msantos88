package br.com.barbearia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;

@Configuration
class SecurityConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

    private static final class SecureRandomHolder {
        private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

        static byte[] bytes(int n) {
            byte[] b = new byte[n];
            RANDOM.nextBytes(b);
            return b;
        }
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SegurancaProperties seguranca, Clock clock) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // O simulador "aprova" pagamentos sem dinheiro: só a barbearia pode usá-lo.
                        .requestMatchers("/admin.html", "/admin.js", "/api/admin/**", "/api/simulador/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                // CSRF só faz sentido para o painel admin (autenticado via navegador).
                // Rotas públicas e o webhook do Mercado Pago não usam sessão/cookie.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(request -> !ProtecaoContraAbusoFilter.exigeLogin(request.getRequestURI())))
                .addFilterBefore(new ProtecaoContraAbusoFilter(seguranca, clock), BasicAuthenticationFilter.class)
                .addFilterAfter(new CarregarCsrfToken(), BasicAuthenticationFilter.class)
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; frame-ancestors 'none'")));
        return http.build();
    }

    @Bean
    UserDetailsService usuarios(@Value("${barbearia.admin.usuario}") String usuario,
                                @Value("${barbearia.admin.senha:}") String senha,
                                PasswordEncoder encoder) {
        if (senha.isBlank()) {
            // Sem senha configurada: gera uma aleatória a cada inicialização (nunca existe senha padrão).
            senha = java.util.HexFormat.of().formatHex(SecureRandomHolder.bytes(12));
            log.warn("\n\n  ADMIN_SENHA não definida. Senha temporária do painel ({}): {}\n", usuario, senha);
        } else if (senha.length() < 10) {
            throw new IllegalStateException("ADMIN_SENHA precisa ter pelo menos 10 caracteres");
        } else if (senha.startsWith("troque-")) {
            throw new IllegalStateException("ADMIN_SENHA ainda é a senha de exemplo do .env.exemplo: troque-a");
        }
        return new InMemoryUserDetailsManager(User.withUsername(usuario)
                .password(encoder.encode(senha)).roles("ADMIN").build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Garante que o cookie XSRF-TOKEN seja enviado ao navegador do admin. */
    private static final class CarregarCsrfToken extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(req, res);
        }
    }
}
