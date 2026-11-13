package br.com.barbearia.application;

import br.com.barbearia.domain.Agendamento;
import br.com.barbearia.domain.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {

    /** Agendamentos do barbeiro que se sobrepõem ao intervalo [inicio, fim). */
    @Query("""
            select a from Agendamento a
             where a.barbeiro.id = :barbeiroId
               and a.status in :status
               and a.inicio < :fim
               and a.fim > :inicio
            """)
    List<Agendamento> buscarSobrepostos(@Param("barbeiroId") Long barbeiroId,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fim") LocalDateTime fim,
                                        @Param("status") Collection<StatusAgendamento> status);

    /** Cliente (por telefone ou e-mail) que já tem uma reserva aguardando pagamento dentro do prazo. */
    @Query("""
            select count(a) > 0 from Agendamento a
             where a.status = br.com.barbearia.domain.StatusAgendamento.AGUARDANDO_PAGAMENTO
               and a.expiraEm > :agora
               and (a.clienteTelefone = :telefone or a.clienteEmail = :email)
            """)
    boolean existePendenteDoCliente(@Param("telefone") String telefone,
                                    @Param("email") String email,
                                    @Param("agora") LocalDateTime agora);

    long countByBarbeiroIdAndStatusInAndInicioAfter(Long barbeiroId, Collection<StatusAgendamento> status,
                                                    LocalDateTime depoisDe);

    Optional<Agendamento> findByPagamentoId(String pagamentoId);

    List<Agendamento> findByStatus(StatusAgendamento status);

    List<Agendamento> findByInicioBetweenOrderByInicio(LocalDateTime de, LocalDateTime ate);
}
