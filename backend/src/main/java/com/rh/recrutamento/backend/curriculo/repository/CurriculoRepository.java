package com.rh.recrutamento.backend.curriculo.repository;

import com.rh.recrutamento.backend.curriculo.entity.Curriculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurriculoRepository extends JpaRepository<Curriculo, Long> {

    boolean existsByUsuario_Id(Long usuarioId);

    Optional<Curriculo> findByUsuario_Id(Long usuarioId);
}
