package br.com.barbearia.adapters.web;

import br.com.barbearia.adapters.payment.SimuladorGateway;
import br.com.barbearia.adapters.web.Dtos.AgendamentoDto;
import br.com.barbearia.application.AgendamentoService;
import br.com.barbearia.domain.Agendamento;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Só existe com o gateway simulado: permite "pagar" o PIX durante o desenvolvimento. */
@RestController
@RequestMapping("/api/simulador")
@ConditionalOnProperty(name = "pagamento.provedor", havingValue = "simulador", matchIfMissing = true)
class SimuladorController {

    private final SimuladorGateway simulador;
    private final AgendamentoService service;
    private final java.time.ZoneId fuso;

    SimuladorController(SimuladorGateway simulador, AgendamentoService service,
                        br.com.barbearia.application.BarbeariaProperties props) {
        this.simulador = simulador;
        this.service = service;
        this.fuso = props.fusoHorario();
    }

    @PostMapping("/agendamentos/{id}/pagar")
    AgendamentoDto pagar(@PathVariable UUID id) {
        Agendamento a = service.buscar(id);
        simulador.aprovar(a.getPagamentoId());
        service.processarPagamento(a.getPagamentoId()); // equivalente ao webhook
        return AgendamentoDto.de(service.buscar(id), fuso);
    }
}
