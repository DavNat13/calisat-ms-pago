package com.califorge.mspago.service;

import com.califorge.mspago.model.EstadoPagoCliente;
import com.califorge.mspago.repository.EstadoPagoClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity Check antifraude: si una misma tarjeta supera {@code maxIntentos} intentos dentro de
 * {@code ventanaSegundos}, se bloquea {@code bloqueoMinutos} (HTTP 429). La ventana deslizante vive
 * en memoria; el bloqueo se persiste en BD para sobrevivir reinicios.
 */
@Service
public class VelocityCheckService {

    private static final Logger log = LoggerFactory.getLogger(VelocityCheckService.class);

    /** Resultado de registrar un intento. */
    public record Resultado(boolean bloqueado, int intentosEnVentana, Instant bloqueadoHasta) {
    }

    private final EstadoPagoClienteRepository repo;
    private final Clock clock;
    private final int maxIntentos;
    private final int ventanaSegundos;
    private final int bloqueoMinutos;
    private final Map<String, Deque<Instant>> ventanas = new ConcurrentHashMap<>();

    @Autowired
    public VelocityCheckService(EstadoPagoClienteRepository repo, Clock clock,
                                @Value("${calisat.pago.antifraude.max-intentos:3}") int maxIntentos,
                                @Value("${calisat.pago.antifraude.ventana-segundos:10}") int ventanaSegundos,
                                @Value("${calisat.pago.antifraude.bloqueo-minutos:30}") int bloqueoMinutos) {
        this.repo = repo;
        this.clock = clock;
        this.maxIntentos = maxIntentos;
        this.ventanaSegundos = ventanaSegundos;
        this.bloqueoMinutos = bloqueoMinutos;
    }

    public int ventanaSegundos() {
        return ventanaSegundos;
    }

    /** Devuelve el instante hasta el que la tarjeta sigue bloqueada, o null si no lo esta (desbloquea si expiro). */
    public Instant bloqueadaHasta(String tarjetaHash) {
        EstadoPagoCliente estado = repo.findById(tarjetaHash).orElse(null);
        if (estado == null || !estado.isBloqueado()) {
            return null;
        }
        Instant ahora = clock.instant();
        if (estado.getBloqueadoHasta() != null && ahora.isAfter(estado.getBloqueadoHasta())) {
            estado.setBloqueado(false);
            estado.setCantidadIntentos(0);
            estado.setBloqueadoHasta(null);
            estado.setActualizadoEn(ahora);
            repo.save(estado);
            ventanas.remove(tarjetaHash);
            log.info("Bloqueo expirado para tarjeta {}: desbloqueada", abreviar(tarjetaHash));
            return null;
        }
        return estado.getBloqueadoHasta();
    }

    /** Registra un intento y decide si corresponde bloquear (intento N+1 dentro de la ventana). */
    public Resultado registrarIntento(String tarjetaHash) {
        Instant ahora = clock.instant();
        Deque<Instant> ventana = ventanas.computeIfAbsent(tarjetaHash, k -> new ArrayDeque<>());
        int total;
        synchronized (ventana) {
            Instant limite = ahora.minusSeconds(ventanaSegundos);
            while (!ventana.isEmpty() && !ventana.peekFirst().isAfter(limite)) {
                ventana.pollFirst();
            }
            ventana.addLast(ahora);
            total = ventana.size();
        }
        EstadoPagoCliente estado = repo.findById(tarjetaHash).orElseGet(() -> {
            EstadoPagoCliente nuevo = new EstadoPagoCliente();
            nuevo.setTarjetaHash(tarjetaHash);
            return nuevo;
        });
        estado.setCantidadIntentos(total);
        estado.setActualizadoEn(ahora);
        if (total > maxIntentos) {
            Instant hasta = ahora.plus(Duration.ofMinutes(bloqueoMinutos));
            estado.setBloqueado(true);
            estado.setBloqueadoHasta(hasta);
            repo.save(estado);
            log.warn("ALERTA FRAUDE: tarjeta {} con {} intentos en {}s; bloqueada hasta {}",
                    abreviar(tarjetaHash), total, ventanaSegundos, hasta);
            return new Resultado(true, total, hasta);
        }
        repo.save(estado);
        return new Resultado(false, total, null);
    }

    /** Reinicia contadores tras un pago exitoso. */
    public void limpiar(String tarjetaHash) {
        ventanas.remove(tarjetaHash);
        repo.findById(tarjetaHash).ifPresent(e -> {
            e.setCantidadIntentos(0);
            repo.save(e);
        });
    }

    private static String abreviar(String hash) {
        return hash.length() > 8 ? hash.substring(0, 8) : hash;
    }
}
