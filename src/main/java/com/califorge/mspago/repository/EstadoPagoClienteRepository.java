package com.califorge.mspago.repository;

import com.califorge.mspago.model.EstadoPagoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoPagoClienteRepository extends JpaRepository<EstadoPagoCliente, String> {
}
