package com.califorge.mspago.repository;

import com.califorge.mspago.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByToken(String token);
    List<Pago> findByClienteSubOrderByCreadoDesc(String clienteSub);
}
