package com.califorge.mspago.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false, length = 60)
    private String ordenId;

    @Column(name = "cliente_sub", nullable = false, length = 150)
    private String clienteSub;

    @Column(nullable = false)
    private Long monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(name = "buy_order", nullable = false, length = 40, unique = true)
    private String buyOrder;

    @Column(length = 100)
    private String token;

    @Column(name = "tarjeta_hash", length = 64)
    private String tarjetaHash;

    @Column(name = "tarjeta_ultimos4", length = 4)
    private String tarjetaUltimos4;

    @Column(name = "codigo_autorizacion", length = 20)
    private String codigoAutorizacion;

    @Column(nullable = false)
    private boolean simulado;

    @Column(nullable = false)
    private Instant creado = Instant.now();

    public Long getId() { return id; }
    public String getOrdenId() { return ordenId; }
    public void setOrdenId(String ordenId) { this.ordenId = ordenId; }
    public String getClienteSub() { return clienteSub; }
    public void setClienteSub(String clienteSub) { this.clienteSub = clienteSub; }
    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }
    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }
    public String getBuyOrder() { return buyOrder; }
    public void setBuyOrder(String buyOrder) { this.buyOrder = buyOrder; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTarjetaHash() { return tarjetaHash; }
    public void setTarjetaHash(String tarjetaHash) { this.tarjetaHash = tarjetaHash; }
    public String getTarjetaUltimos4() { return tarjetaUltimos4; }
    public void setTarjetaUltimos4(String tarjetaUltimos4) { this.tarjetaUltimos4 = tarjetaUltimos4; }
    public String getCodigoAutorizacion() { return codigoAutorizacion; }
    public void setCodigoAutorizacion(String codigoAutorizacion) { this.codigoAutorizacion = codigoAutorizacion; }
    public boolean isSimulado() { return simulado; }
    public void setSimulado(boolean simulado) { this.simulado = simulado; }
    public Instant getCreado() { return creado; }
}
