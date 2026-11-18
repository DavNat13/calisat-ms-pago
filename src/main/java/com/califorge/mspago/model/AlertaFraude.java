package com.califorge.mspago.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "alertas_fraude")
public class AlertaFraude {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tarjeta_hash", nullable = false, length = 64)
    private String tarjetaHash;

    @Column(name = "tarjeta_ultimos4", length = 4)
    private String tarjetaUltimos4;

    @Column(name = "cliente_sub", length = 150)
    private String clienteSub;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ventana_segundos", nullable = false)
    private int ventanaSegundos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(nullable = false)
    private Instant creada = Instant.now();

    public Long getId() { return id; }
    public String getTarjetaHash() { return tarjetaHash; }
    public void setTarjetaHash(String tarjetaHash) { this.tarjetaHash = tarjetaHash; }
    public String getTarjetaUltimos4() { return tarjetaUltimos4; }
    public void setTarjetaUltimos4(String tarjetaUltimos4) { this.tarjetaUltimos4 = tarjetaUltimos4; }
    public String getClienteSub() { return clienteSub; }
    public void setClienteSub(String clienteSub) { this.clienteSub = clienteSub; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public int getVentanaSegundos() { return ventanaSegundos; }
    public void setVentanaSegundos(int ventanaSegundos) { this.ventanaSegundos = ventanaSegundos; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Instant bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public Instant getCreada() { return creada; }
    public void setCreada(Instant creada) { this.creada = creada; }
}
