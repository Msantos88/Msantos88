package br.com.barbearia.adapters.web;

import br.com.barbearia.adapters.payment.MercadoPagoAssinatura;
import br.com.barbearia.adapters.payment.MercadoPagoProperties;
import br.com.barbearia.application.AgendamentoService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagamentos/webhook")
@ConditionalOnProperty(name = "pagamento.provedor", havingValue = "mercadopago")
class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final AgendamentoService service;
    private final MercadoPagoProperties props;

    WebhookController(AgendamentoService service, MercadoPagoProperties props) {
        this.service = service;
        this.props = props;
    }

    @PostMapping("/mercadopago")
    ResponseEntity<Void> mercadoPago(@RequestParam(name = "data.id", required = false) String dataIdQuery,
                                     @RequestParam(name = "type", required = false) String tipoQuery,
                                     @RequestHeader(name = "x-signature", required = false) String assinatura,
                                     @RequestHeader(name = "x-request-id", required = false) String requestId,
                                     @RequestBody(required = false) JsonNode corpo) {
        String tipo = tipoQuery != null ? tipoQuery : corpo == null ? null : corpo.path("type").asText(null);
        String dataId = dataIdQuery != null ? dataIdQuery : corpo == null ? null : corpo.path("data").path("id").asText(null);
        if (!"payment".equals(tipo) || dataId == null) {
            return ResponseEntity.ok().build(); // outros eventos são ignorados
        }
        if (StringUtils.hasText(props.webhookSecret())
                && !MercadoPagoAssinatura.valida(props.webhookSecret(), assinatura, requestId, dataId)) {
            log.warn("Webhook com assinatura inválida para pagamento {}", dataId);
            return ResponseEntity.status(401).build();
        }
        // Mesmo com assinatura válida, o status é sempre confirmado consultando a API do Mercado Pago.
        service.processarPagamento(dataId);
        return ResponseEntity.ok().build();
    }
}
