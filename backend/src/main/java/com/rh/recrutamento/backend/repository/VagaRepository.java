package com.rh.recrutamento.backend.repository;

import com.rh.recrutamento.backend.entity.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    List<Vaga> findByRh_Id(Long rhId);

    List<Vaga> findByStatusNot(Vaga.Status status);
}
