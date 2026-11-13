package br.com.barbearia.adapters.web;

import br.com.barbearia.adapters.payment.SimuladorGateway;
import br.com.barbearia.adapters.web.Dtos.AgendamentoDto;
import br.com.barbearia.adapters.web.Dtos.BarbeiroDto;
import br.com.barbearia.adapters.web.Dtos.InfoDto;
import br.com.barbearia.adapters.web.Dtos.NovoAgendamentoRequest;
import br.com.barbearia.adapters.web.Dtos.ServicoDto;
import br.com.barbearia.application.AgendamentoService;
import br.com.barbearia.application.AgendamentoService.NovoAgendamento;
import br.com.barbearia.application.BarbeariaProperties;
import br.com.barbearia.application.BarbeiroRepository;
import br.com.barbearia.application.ServicoRepository;
import br.com.barbearia.domain.PoliticaDeSinal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
class PublicoController {

    private final AgendamentoService service;
    private final ServicoRepository servicos;
    private final BarbeiroRepository barbeiros;
    private final BarbeariaProperties props;
    private final PoliticaDeSinal politica;
    private final boolean simulado;
    private final java.time.ZoneId fuso;

    PublicoController(AgendamentoService service, ServicoRepository servicos, BarbeiroRepository barbeiros,
                      BarbeariaProperties props, java.util.Optional<SimuladorGateway> simulador) {
        this.service = service;
        this.servicos = servicos;
        this.barbeiros = barbeiros;
        this.props = props;
        this.politica = new PoliticaDeSinal(props.percentualSinal());
        this.simulado = simulador.isPresent();
        this.fuso = props.fusoHorario();
    }

    @GetMapping("/info")
    InfoDto info() {
        return new InfoDto(props.nome(), props.slogan(), props.whatsapp(), props.instagram(),
                props.endereco(), props.bairroCidade(), props.horarioAtendimento(), props.percentualSinal(), props.minutosParaPagarSinal(),
                props.diasMaximosParaAgendar(), props.diasDeFuncionamento(), simulado);
    }

    @GetMapping("/servicos")
    List<ServicoDto> servicos() {
        return servicos.findByAtivoTrueOrderById().stream()
                .map(s -> Dtos.servico(s, politica.calcularSinal(s.getPreco())))
                .toList();
    }

    @GetMapping("/barbeiros")
    List<BarbeiroDto> barbeiros() {
        return barbeiros.findByAtivoTrueOrderByNome().stream().map(BarbeiroDto::de).toList();
    }

    @GetMapping("/horarios")
    List<LocalTime> horarios(@RequestParam Long barbeiroId, @RequestParam Long servicoId,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.horariosDisponiveis(barbeiroId, servicoId, data);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    AgendamentoDto agendar(@Valid @RequestBody NovoAgendamentoRequest r) {
        return AgendamentoDto.de(service.reservar(new NovoAgendamento(r.servicoId(), r.barbeiroId(), r.inicio(),
                r.clienteNome(), r.clienteTelefone(), r.clienteEmail())), fuso);
    }

    @GetMapping("/agendamentos/{id}")
    AgendamentoDto consultar(@PathVariable UUID id) {
        return AgendamentoDto.de(service.buscar(id), fuso);
    }
}
