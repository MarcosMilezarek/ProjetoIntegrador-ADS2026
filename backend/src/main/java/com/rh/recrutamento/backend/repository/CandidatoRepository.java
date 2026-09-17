package com.rh.recrutamento.backend.repository;

import com.rh.recrutamento.backend.entity.Candidato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidatoRepository extends JpaRepository<Candidato, Long> {
}
