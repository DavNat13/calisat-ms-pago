package com.califorge.mspago.repository;

import com.califorge.mspago.model.AlertaFraude;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaFraudeRepository extends JpaRepository<AlertaFraude, Long> {
    List<AlertaFraude> findTop50ByOrderByCreadaDesc();
}
