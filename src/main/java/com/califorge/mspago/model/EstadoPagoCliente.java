package com.califorge.mspago.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Estado antifraude por tarjeta (clave = huella SHA-256): bloqueo vigente y contador. */
@Entity
@Table(name = "estado_pago_tarjeta")
public class EstadoPagoCliente {

    @Id
    @Column(name = "tarjeta_hash", length = 64)
    private String tarjetaHash;

    @Column(name = "cantidad_intentos", nullable = false)
    private int cantidadIntentos;

    @Column(nullable = false)
    private boolean bloqueado;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    public String getTarjetaHash() { return tarjetaHash; }
    public void setTarjetaHash(String tarjetaHash) { this.tarjetaHash = tarjetaHash; }
    public int getCantidadIntentos() { return cantidadIntentos; }
    public void setCantidadIntentos(int cantidadIntentos) { this.cantidadIntentos = cantidadIntentos; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Instant bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
