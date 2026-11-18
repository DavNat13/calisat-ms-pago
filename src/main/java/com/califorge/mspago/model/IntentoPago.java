package com.califorge.mspago.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Auditoria de cada intento de pago consumido desde Kafka (payment-attempts). */
@Entity
@Table(name = "intentos_pago")
public class IntentoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_transaccion", nullable = false, unique = true, length = 60)
    private String idTransaccion;

    @Column(name = "tarjeta_hash", nullable = false, length = 64)
    private String tarjetaHash;

    @Column(name = "cliente_sub", length = 150)
    private String clienteSub;

    private Long monto;

    @Column(nullable = false)
    private boolean bloqueado;

    @Column(nullable = false)
    private Instant momento;

    public Long getId() { return id; }
    public String getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(String idTransaccion) { this.idTransaccion = idTransaccion; }
    public String getTarjetaHash() { return tarjetaHash; }
    public void setTarjetaHash(String tarjetaHash) { this.tarjetaHash = tarjetaHash; }
    public String getClienteSub() { return clienteSub; }
    public void setClienteSub(String clienteSub) { this.clienteSub = clienteSub; }
    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public Instant getMomento() { return momento; }
    public void setMomento(Instant momento) { this.momento = momento; }
}
