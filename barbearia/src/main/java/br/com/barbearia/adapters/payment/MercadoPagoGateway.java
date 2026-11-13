package br.com.barbearia.adapters.payment;

import br.com.barbearia.application.GatewayPagamento;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cobrança PIX via API de pagamentos do Mercado Pago.
 * Docs: https://www.mercadopago.com.br/developers/pt/docs/checkout-api/integration-configuration/integrate-with-pix
 */
@Component
@ConditionalOnProperty(name = "pagamento.provedor", havingValue = "mercadopago")
public class MercadoPagoGateway implements GatewayPagamento {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final RestClient http;
    private final MercadoPagoProperties props;

    public MercadoPagoGateway(MercadoPagoProperties props, RestClient.Builder builder) {
        if (!StringUtils.hasText(props.accessToken())) {
            throw new IllegalStateException("Defina MERCADOPAGO_ACCESS_TOKEN para usar o Mercado Pago");
        }
        this.props = props;
        this.http = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.accessToken())
                .build();
    }

    @Override
    public CobrancaPix criarCobrancaPix(NovaCobranca c) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("transaction_amount", c.valor());
        corpo.put("description", c.descricao());
        corpo.put("payment_method_id", "pix");
        corpo.put("external_reference", c.referencia());
        corpo.put("date_of_expiration", FORMATO_DATA.format(c.expiraEm()));
        corpo.put("payer", Map.of("email", c.pagadorEmail(), "first_name", c.pagadorNome()));
        if (StringUtils.hasText(props.notificationUrl())) {
            corpo.put("notification_url", props.notificationUrl());
        }

        JsonNode resposta = http.post()
                .uri("/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                // mesma referência => mesma cobrança, caso a requisição seja repetida
                .header("X-Idempotency-Key", c.referencia())
                .body(corpo)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    String detalhe = new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    throw new IllegalStateException("Mercado Pago recusou a criação do PIX (HTTP "
                            + res.getStatusCode().value() + "): " + detalhe);
                })
                .body(JsonNode.class);

        JsonNode dados = resposta.path("point_of_interaction").path("transaction_data");
        return new CobrancaPix(
                resposta.path("id").asText(),
                dados.path("qr_code").asText(null),
                dados.path("qr_code_base64").asText(null),
                dados.path("ticket_url").asText(null));
    }

    @Override
    public PagamentoConsultado consultar(String pagamentoId) {
        JsonNode p = http.get()
                .uri("/v1/payments/{id}", pagamentoId)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    String detalhe = new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    throw new IllegalStateException("Mercado Pago recusou a consulta do pagamento " + pagamentoId
                            + " (HTTP " + res.getStatusCode().value() + "): " + detalhe);
                })
                .body(JsonNode.class);
        return new PagamentoConsultado(
                p.path("id").asText(),
                p.path("external_reference").asText(null),
                p.hasNonNull("transaction_amount") ? new BigDecimal(p.get("transaction_amount").asText()) : null,
                traduzir(p.path("status").asText()));
    }

    private static StatusPagamento traduzir(String status) {
        return switch (status) {
            case "approved" -> StatusPagamento.APROVADO;
            case "rejected", "cancelled" -> StatusPagamento.RECUSADO_OU_CANCELADO;
            case "refunded", "charged_back" -> StatusPagamento.ESTORNADO;
            default -> StatusPagamento.PENDENTE; // pending, in_process, authorized...
        };
    }
}
