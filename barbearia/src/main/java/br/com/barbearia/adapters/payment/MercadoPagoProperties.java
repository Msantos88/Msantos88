package br.com.barbearia.adapters.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pagamento.mercadopago")
public record MercadoPagoProperties(
        String baseUrl,
        /** Access token da conta (APP_USR-... em produção, TEST-... em testes). */
        String accessToken,
        /** Chave secreta do webhook (painel do Mercado Pago > Suas integrações > Webhooks). */
        String webhookSecret,
        /** URL pública que o Mercado Pago chamará, ex.: https://seusite.com.br/api/pagamentos/webhook/mercadopago */
        String notificationUrl) {
}
