package br.com.barbearia.application;

import br.com.barbearia.application.GatewayPagamento.CobrancaPix;
import br.com.barbearia.application.GatewayPagamento.NovaCobranca;
import br.com.barbearia.application.GatewayPagamento.PagamentoConsultado;
import br.com.barbearia.domain.Agendamento;
import br.com.barbearia.domain.Barbeiro;
import br.com.barbearia.domain.PoliticaDeSinal;
import br.com.barbearia.domain.Servico;
import br.com.barbearia.domain.StatusAgendamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgendamentoService {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);

    private static final List<StatusAgendamento> STATUS_QUE_OCUPAM = Arrays.stream(StatusAgendamento.values())
            .filter(StatusAgendamento::ocupaHorario).toList();

    static final int VENCIMENTO_MINIMO_PIX_MINUTOS = 35;

    private final AgendamentoRepository agendamentos;
    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;
    private final GatewayPagamento gateway;
    private final BarbeariaProperties props;
    private final PoliticaDeSinal politicaDeSinal;
    private final Clock clock;
    private final TransactionTemplate tx;

    public AgendamentoService(AgendamentoRepository agendamentos, BarbeiroRepository barbeiros,
                              ServicoRepository servicos, GatewayPagamento gateway,
                              BarbeariaProperties props, Clock clock, TransactionTemplate tx) {
        this.agendamentos = agendamentos;
        this.barbeiros = barbeiros;
        this.servicos = servicos;
        this.gateway = gateway;
        this.props = props;
        this.politicaDeSinal = new PoliticaDeSinal(props.percentualSinal());
        this.clock = clock;
        this.tx = tx;
    }

    public record NovoAgendamento(Long servicoId, Long barbeiroId, LocalDateTime inicio,
                                  String clienteNome, String clienteTelefone, String clienteEmail) {
    }

    // ------------------------------------------------------------------ consulta de horários

    public List<LocalTime> horariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        Servico servico = servicoAtivo(servicoId);
        barbeiroAtivo(barbeiroId);
        LocalDateTime agora = agora();

        if (!diaPermitido(data, agora)) {
            return List.of();
        }
        LocalDateTime inicioDoDia = data.atTime(props.abertura());
        LocalDateTime fimDoDia = data.atTime(props.fechamento());
        List<Agendamento> ocupados = ocupando(barbeiroId, inicioDoDia, fimDoDia, agora, null);

        List<LocalTime> livres = new ArrayList<>();
        for (LocalDateTime inicio = inicioDoDia;
             !inicio.plusMinutes(servico.getDuracaoMinutos()).isAfter(fimDoDia);
             inicio = inicio.plusMinutes(props.intervaloEntreHorariosMinutos())) {
            LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());
            LocalDateTime candidato = inicio;
            boolean conflita = ocupados.stream()
                    .anyMatch(a -> a.getInicio().isBefore(fim) && a.getFim().isAfter(candidato));
            if (!conflita && respeitaAntecedencia(inicio, agora)) {
                livres.add(inicio.toLocalTime());
            }
        }
        return livres;
    }

    // ------------------------------------------------------------------ reserva + cobrança do sinal

    /**
     * Reserva o horário provisoriamente e gera a cobrança PIX do sinal.
     * O horário fica bloqueado por {@code minutosParaPagarSinal}; se o sinal
     * não for pago nesse prazo, a reserva expira e o horário volta a ficar livre.
     */
    public Agendamento reservar(NovoAgendamento cmd) {
        Agendamento reservado = tx.execute(status -> reservarHorario(cmd));

        // O Mercado Pago exige vencimento do PIX de pelo menos 30 minutos a partir do momento em que
        // recebe o pedido. Damos uma folga: se o PIX for pago depois que a reserva expirou, o pagamento
        // ainda é tratado (confirma se o horário estiver livre ou marca para devolver o sinal).
        LocalDateTime vencimentoMinimo = agora().plusMinutes(VENCIMENTO_MINIMO_PIX_MINUTOS);
        LocalDateTime vencimentoPix = reservado.getExpiraEm().isAfter(vencimentoMinimo)
                ? reservado.getExpiraEm() : vencimentoMinimo;

        CobrancaPix cobranca;
        try {
            cobranca = gateway.criarCobrancaPix(new NovaCobranca(
                    reservado.getId().toString(),
                    reservado.getValorSinal(),
                    "Sinal " + props.percentualSinal() + "% - " + reservado.getServico().getNome()
                            + " em " + reservado.getInicio().toLocalDate(),
                    reservado.getClienteNome(),
                    reservado.getClienteEmail(),
                    vencimentoPix.atZone(props.fusoHorario()).toOffsetDateTime()));
        } catch (RuntimeException e) {
            log.error("Falha ao gerar cobrança do agendamento {}: {}", reservado.getId(), e.getMessage(), e);
            tx.executeWithoutResult(status -> agendamentos.findById(reservado.getId()).ifPresent(a -> {
                a.cancelar();
                agendamentos.save(a);
            }));
            throw new RegraDeNegocioException("Não foi possível gerar o pagamento agora. Tente novamente em instantes.");
        }

        return tx.execute(status -> {
            Agendamento a = buscar(reservado.getId());
            a.registrarCobranca(cobranca.pagamentoId(), cobranca.copiaECola(), cobranca.qrCodeBase64(), cobranca.url());
            return agendamentos.save(a);
        });
    }

    private Agendamento reservarHorario(NovoAgendamento cmd) {
        Servico servico = servicoAtivo(cmd.servicoId());
        Barbeiro barbeiro = barbeiros.travarPorId(cmd.barbeiroId())
                .filter(Barbeiro::isAtivo)
                .orElseThrow(() -> new NaoEncontradoException("Barbeiro não encontrado"));

        LocalDateTime agora = agora();
        LocalDateTime inicio = cmd.inicio().withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());
        validarHorario(inicio, fim, agora);

        String telefone = cmd.clienteTelefone().replaceAll("\\D", "");
        String email = cmd.clienteEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (telefone.length() < 10 || telefone.length() > 13) {
            throw new RegraDeNegocioException("Telefone inválido. Informe o DDD e o número.");
        }
        // Cada cliente só pode segurar um horário por vez sem pagar: evita que alguém bloqueie a agenda.
        if (agendamentos.existePendenteDoCliente(telefone, email, agora)) {
            throw new RegraDeNegocioException("Você já tem uma reserva aguardando pagamento. Pague o PIX dela "
                    + "ou aguarde " + props.minutosParaPagarSinal() + " minutos para ela expirar.");
        }
        if (!ocupando(barbeiro.getId(), inicio, fim, agora, null).isEmpty()) {
            throw new RegraDeNegocioException("Este horário acabou de ser reservado. Escolha outro horário.");
        }

        LocalDateTime expiraEm = agora.plusMinutes(props.minutosParaPagarSinal());
        Agendamento agendamento = new Agendamento(servico, barbeiro, cmd.clienteNome().trim(),
                telefone, email, inicio,
                politicaDeSinal.calcularSinal(servico.getPreco()), agora, expiraEm);
        return agendamentos.save(agendamento);
    }

    // ------------------------------------------------------------------ pagamento

    /**
     * Chamado pelo webhook do provedor e pela reconciliação periódica.
     * Nunca confia no conteúdo da notificação: sempre consulta o pagamento no provedor.
     */
    public void processarPagamento(String pagamentoId) {
        PagamentoConsultado pagamento = gateway.consultar(pagamentoId);
        tx.executeWithoutResult(status -> aplicarPagamento(pagamento));
    }

    private void aplicarPagamento(PagamentoConsultado pagamento) {
        Optional<Agendamento> encontrado = agendamentos.findByPagamentoId(pagamento.pagamentoId());
        if (encontrado.isEmpty()) {
            log.warn("Pagamento {} não corresponde a nenhum agendamento", pagamento.pagamentoId());
            return;
        }
        Agendamento a = encontrado.get();
        if (pagamento.referencia() != null && !pagamento.referencia().equals(a.getId().toString())) {
            log.warn("Pagamento {} com referência divergente ({})", pagamento.pagamentoId(), pagamento.referencia());
            return;
        }
        LocalDateTime agora = agora();

        switch (pagamento.status()) {
            case APROVADO -> {
                if (pagamento.valor() == null || pagamento.valor().compareTo(a.getValorSinal()) < 0) {
                    log.warn("Pagamento {} com valor {} menor que o sinal {}", pagamento.pagamentoId(),
                            pagamento.valor(), a.getValorSinal());
                    return;
                }
                boolean dentroDoPrazo = a.getStatus() == StatusAgendamento.AGUARDANDO_PAGAMENTO
                        && !a.isPendenteExpirado(agora);
                boolean foraDoPrazo = a.getStatus() == StatusAgendamento.EXPIRADO
                        || (a.getStatus() == StatusAgendamento.AGUARDANDO_PAGAMENTO && a.isPendenteExpirado(agora));
                if (dentroDoPrazo) {
                    a.confirmarPagamento(agora);
                } else if (foraDoPrazo) {
                    // Pagou depois do prazo: confirma se o horário ainda estiver livre.
                    barbeiros.travarPorId(a.getBarbeiro().getId());
                    boolean livre = ocupando(a.getBarbeiro().getId(), a.getInicio(), a.getFim(), agora, a.getId()).isEmpty();
                    if (livre && a.getInicio().isAfter(agora)) {
                        a.confirmarPagamento(agora);
                    } else {
                        a.marcarReembolsoPendente(agora);
                        log.warn("Agendamento {} pago após expirar e horário ocupado: reembolso pendente", a.getId());
                    }
                } else {
                    return; // já processado (notificação repetida) ou cancelado manualmente
                }
                agendamentos.save(a);
                log.info("Agendamento {} -> {}", a.getId(), a.getStatus());
            }
            case RECUSADO_OU_CANCELADO -> {
                if (a.getStatus() == StatusAgendamento.AGUARDANDO_PAGAMENTO) {
                    a.expirar();
                    agendamentos.save(a);
                }
            }
            case PENDENTE, ESTORNADO -> {
                // nada a fazer: pendentes expiram sozinhos; estornos são tratados manualmente
            }
        }
    }

    /** Confere pagamentos pendentes (caso algum webhook tenha se perdido) e libera horários vencidos. */
    public void verificarPendentes() {
        for (Agendamento pendente : agendamentos.findByStatus(StatusAgendamento.AGUARDANDO_PAGAMENTO)) {
            try {
                if (pendente.getPagamentoId() != null) {
                    processarPagamento(pendente.getPagamentoId());
                }
                tx.executeWithoutResult(status -> agendamentos.findById(pendente.getId())
                        .filter(a -> a.isPendenteExpirado(agora()))
                        .ifPresent(a -> {
                            a.expirar();
                            agendamentos.save(a);
                            log.info("Agendamento {} expirou sem pagamento do sinal", a.getId());
                        }));
            } catch (RuntimeException e) {
                log.error("Erro ao verificar agendamento pendente {}", pendente.getId(), e);
            }
        }
    }

    // ------------------------------------------------------------------ administração

    public Agendamento buscar(UUID id) {
        return agendamentos.findById(id).orElseThrow(() -> new NaoEncontradoException("Agendamento não encontrado"));
    }

    public List<Agendamento> listarDoDia(LocalDate data) {
        return agendamentos.findByInicioBetweenOrderByInicio(data.atStartOfDay(), data.plusDays(1).atStartOfDay());
    }

    public Agendamento cancelar(UUID id) {
        return tx.execute(status -> {
            Agendamento a = buscar(id);
            try {
                a.cancelar();
            } catch (IllegalStateException e) {
                throw new RegraDeNegocioException(e.getMessage());
            }
            return agendamentos.save(a);
        });
    }

    public Agendamento concluir(UUID id) {
        return tx.execute(status -> {
            Agendamento a = buscar(id);
            try {
                a.concluir();
            } catch (IllegalStateException e) {
                throw new RegraDeNegocioException(e.getMessage());
            }
            return agendamentos.save(a);
        });
    }

    // ------------------------------------------------------------------ auxiliares

    private void validarHorario(LocalDateTime inicio, LocalDateTime fim, LocalDateTime agora) {
        LocalDate data = inicio.toLocalDate();
        if (!diaPermitido(data, agora)) {
            throw new RegraDeNegocioException("A barbearia não atende nesta data.");
        }
        if (inicio.toLocalTime().isBefore(props.abertura()) || fim.isAfter(data.atTime(props.fechamento()))) {
            throw new RegraDeNegocioException("Horário fora do expediente.");
        }
        long minutosDesdeAbertura = Duration.between(data.atTime(props.abertura()), inicio).toMinutes();
        if (minutosDesdeAbertura % props.intervaloEntreHorariosMinutos() != 0) {
            throw new RegraDeNegocioException("Horário inválido.");
        }
        if (!respeitaAntecedencia(inicio, agora)) {
            throw new RegraDeNegocioException("Agendamentos precisam ser feitos com pelo menos "
                    + props.antecedenciaMinimaMinutos() + " minutos de antecedência.");
        }
    }

    private boolean diaPermitido(LocalDate data, LocalDateTime agora) {
        LocalDate hoje = agora.toLocalDate();
        return !data.isBefore(hoje)
                && !data.isAfter(hoje.plusDays(props.diasMaximosParaAgendar()))
                && props.diasDeFuncionamento().contains(data.getDayOfWeek());
    }

    private boolean respeitaAntecedencia(LocalDateTime inicio, LocalDateTime agora) {
        return !inicio.isBefore(agora.plusMinutes(props.antecedenciaMinimaMinutos()));
    }

    /** Agendamentos que realmente bloqueiam o intervalo (ignora pendentes cujo prazo já venceu). */
    private List<Agendamento> ocupando(Long barbeiroId, LocalDateTime inicio, LocalDateTime fim,
                                       LocalDateTime agora, UUID ignorar) {
        return agendamentos.buscarSobrepostos(barbeiroId, inicio, fim, STATUS_QUE_OCUPAM).stream()
                .filter(a -> !a.isPendenteExpirado(agora))
                .filter(a -> !a.getId().equals(ignorar))
                .toList();
    }

    private Servico servicoAtivo(Long id) {
        return servicos.findById(id).filter(Servico::isAtivo)
                .orElseThrow(() -> new NaoEncontradoException("Serviço não encontrado"));
    }

    private Barbeiro barbeiroAtivo(Long id) {
        return barbeiros.findById(id).filter(Barbeiro::isAtivo)
                .orElseThrow(() -> new NaoEncontradoException("Barbeiro não encontrado"));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
