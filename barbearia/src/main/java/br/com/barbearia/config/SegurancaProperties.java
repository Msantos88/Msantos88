package br.com.barbearia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "barbearia.seguranca")
public record SegurancaProperties(
        /** Requisições à API por IP por minuto (todas as rotas /api). */
        int requisicoesPorMinuto,
        /** Reservas que um mesmo IP pode criar dentro de {@code janelaReservas}. */
        int reservasPorIp,
        Duration janelaReservas,
        /** Senhas erradas no painel antes de bloquear o IP por {@code bloqueioLogin}. */
        int tentativasDeLogin,
        Duration bloqueioLogin) {
}
