package com.rh.recrutamento.backend.analise.repository;

import com.rh.recrutamento.backend.analise.entity.AnaliseCandidatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnaliseCandidaturaRepository extends JpaRepository<AnaliseCandidatura, Long> {

    Optional<AnaliseCandidatura> findByCandidatura_Id(Long candidaturaId);

    List<AnaliseCandidatura> findByCandidatura_IdIn(Collection<Long> candidaturaIds);
}
