package com.rh.recrutamento.backend.repository;

import com.rh.recrutamento.backend.entity.Curriculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurriculoRepository extends JpaRepository<Curriculo, Long> {

    boolean existsByCandidato_UsuarioId(Long usuarioId);

    Optional<Curriculo> findByCandidato_UsuarioId(Long usuarioId);
}
