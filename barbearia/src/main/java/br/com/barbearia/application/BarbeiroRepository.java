package br.com.barbearia.application;

import br.com.barbearia.domain.Barbeiro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {

    List<Barbeiro> findByAtivoTrueOrderByNome();

    List<Barbeiro> findAllByOrderByAtivoDescNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    /**
     * Trava a linha do barbeiro durante a transação para serializar reservas
     * concorrentes e impedir que dois clientes peguem o mesmo horário.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barbeiro b where b.id = :id")
    Optional<Barbeiro> travarPorId(@Param("id") Long id);
}
