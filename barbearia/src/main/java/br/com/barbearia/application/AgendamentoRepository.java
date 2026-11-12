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

    Optional<Agendamento> findByPagamentoId(String pagamentoId);

    List<Agendamento> findByStatus(StatusAgendamento status);

    List<Agendamento> findByInicioBetweenOrderByInicio(LocalDateTime de, LocalDateTime ate);
}
