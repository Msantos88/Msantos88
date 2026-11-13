package br.com.barbearia.application;

import br.com.barbearia.domain.Barbeiro;
import br.com.barbearia.domain.StatusAgendamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Cadastro de barbeiros feito pela barbearia no painel. */
@Service
public class BarbeiroService {

    private static final List<StatusAgendamento> COMPROMISSOS =
            List.of(StatusAgendamento.CONFIRMADO, StatusAgendamento.AGUARDANDO_PAGAMENTO);

    private final BarbeiroRepository barbeiros;
    private final AgendamentoRepository agendamentos;
    private final Clock clock;

    public BarbeiroService(BarbeiroRepository barbeiros, AgendamentoRepository agendamentos, Clock clock) {
        this.barbeiros = barbeiros;
        this.agendamentos = agendamentos;
        this.clock = clock;
    }

    public List<Barbeiro> listarTodos() {
        return barbeiros.findAllByOrderByAtivoDescNomeAsc();
    }

    @Transactional
    public Barbeiro cadastrar(String nome) {
        String limpo = normalizar(nome);
        if (barbeiros.existsByNomeIgnoreCase(limpo)) {
            throw new RegraDeNegocioException("Já existe um barbeiro chamado " + limpo + ".");
        }
        return barbeiros.save(new Barbeiro(limpo));
    }

    @Transactional
    public Barbeiro renomear(Long id, String nome) {
        Barbeiro b = buscar(id);
        String limpo = normalizar(nome);
        if (!b.getNome().equalsIgnoreCase(limpo) && barbeiros.existsByNomeIgnoreCase(limpo)) {
            throw new RegraDeNegocioException("Já existe um barbeiro chamado " + limpo + ".");
        }
        b.renomear(limpo);
        return b;
    }

    /**
     * Tira o barbeiro da página de agendamento. O histórico é mantido.
     * Não permite se ainda houver clientes agendados com ele.
     */
    @Transactional
    public Barbeiro desativar(Long id) {
        Barbeiro b = barbeiros.travarPorId(id).orElseThrow(() -> new NaoEncontradoException("Barbeiro não encontrado"));
        long futuros = agendamentos.countByBarbeiroIdAndStatusInAndInicioAfter(id, COMPROMISSOS, LocalDateTime.now(clock));
        if (futuros > 0) {
            throw new RegraDeNegocioException(b.getNome() + " ainda tem " + futuros + " agendamento(s) futuro(s). "
                    + "Cancele ou conclua antes de desativar.");
        }
        if (barbeiros.findByAtivoTrueOrderByNome().stream().allMatch(x -> x.getId().equals(id))) {
            throw new RegraDeNegocioException("É preciso ter pelo menos um barbeiro ativo.");
        }
        b.desativar();
        return b;
    }

    @Transactional
    public Barbeiro ativar(Long id) {
        Barbeiro b = buscar(id);
        b.ativar();
        return b;
    }

    private Barbeiro buscar(Long id) {
        return barbeiros.findById(id).orElseThrow(() -> new NaoEncontradoException("Barbeiro não encontrado"));
    }

    private static String normalizar(String nome) {
        String limpo = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (limpo.isEmpty() || limpo.length() > 60) {
            throw new RegraDeNegocioException("Informe um nome de até 60 caracteres.");
        }
        return limpo;
    }
}
