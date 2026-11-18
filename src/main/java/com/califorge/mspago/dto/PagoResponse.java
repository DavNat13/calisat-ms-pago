package com.califorge.mspago.dto;

import com.califorge.mspago.model.EstadoPago;
import com.califorge.mspago.model.Pago;

import java.time.Instant;

public record PagoResponse(Long id, String ordenId, Long monto, EstadoPago estado,
                           String tarjetaUltimos4, String codigoAutorizacion, Instant creado) {

    public static PagoResponse de(Pago p) {
        return new PagoResponse(p.getId(), p.getOrdenId(), p.getMonto(), p.getEstado(),
                p.getTarjetaUltimos4(), p.getCodigoAutorizacion(), p.getCreado());
    }
}
