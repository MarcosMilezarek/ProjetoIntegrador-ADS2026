package com.rh.recrutamento.backend.candidatura.repository;

import com.rh.recrutamento.backend.candidatura.entity.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
}
