package com.califorge.mspago.repository;

import com.califorge.mspago.model.IntentoPago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentoPagoRepository extends JpaRepository<IntentoPago, Long> {
    boolean existsByIdTransaccion(String idTransaccion);
}
