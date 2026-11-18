package com.califorge.mspago;

import com.califorge.mspago.model.EstadoPagoCliente;
import com.califorge.mspago.repository.EstadoPagoClienteRepository;
import com.califorge.mspago.service.VelocityCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VelocityCheckServiceTest {

    /** Reloj mutable para simular el paso del tiempo. */
    static class RelojMutable extends Clock {
        Instant ahora = Instant.parse("2026-11-18T12:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
        void avanzar(Duration d) { ahora = ahora.plus(d); }
    }

    private final Map<String, EstadoPagoCliente> bd = new HashMap<>();
    private final RelojMutable reloj = new RelojMutable();
    private VelocityCheckService service;

    @BeforeEach
    void setUp() {
        EstadoPagoClienteRepository repo = mock(EstadoPagoClienteRepository.class);
        when(repo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(bd.get(i.<String>getArgument(0))));
        when(repo.save(any(EstadoPagoCliente.class))).thenAnswer(i -> {
            EstadoPagoCliente e = i.getArgument(0);
            bd.put(e.getTarjetaHash(), e);
            return e;
        });
        service = new VelocityCheckService(repo, reloj, 3, 10, 30);
    }

    @Test
    void tresIntentosEnLaVentanaSonPermitidos() {
        for (int i = 0; i < 3; i++) {
            assertFalse(service.registrarIntento("tarjeta-A").bloqueado());
            reloj.avanzar(Duration.ofSeconds(1));
        }
        assertNull(service.bloqueadaHasta("tarjeta-A"));
    }

    @Test
    void elCuartoIntentoEnDiezSegundosBloqueaPorTreintaMinutos() {
        for (int i = 0; i < 3; i++) {
            service.registrarIntento("tarjeta-A");
            reloj.avanzar(Duration.ofSeconds(2));
        }
        VelocityCheckService.Resultado r = service.registrarIntento("tarjeta-A");
        assertTrue(r.bloqueado());
        assertEquals(4, r.intentosEnVentana());
        assertEquals(reloj.instant().plus(Duration.ofMinutes(30)), r.bloqueadoHasta());
        assertNotNull(service.bloqueadaHasta("tarjeta-A"));
    }

    @Test
    void intentosFueraDeLaVentanaNoAcumulan() {
        for (int i = 0; i < 6; i++) {
            assertFalse(service.registrarIntento("tarjeta-A").bloqueado());
            reloj.avanzar(Duration.ofSeconds(4));   // solo ~3 caben en 10 s
        }
    }

    @Test
    void tarjetasDistintasNoSeAfectan() {
        for (int i = 0; i < 4; i++) {
            service.registrarIntento("tarjeta-A");
        }
        assertFalse(service.registrarIntento("tarjeta-B").bloqueado());
    }

    @Test
    void elBloqueoExpiraTrasTreintaMinutos() {
        for (int i = 0; i < 4; i++) {
            service.registrarIntento("tarjeta-A");
        }
        assertNotNull(service.bloqueadaHasta("tarjeta-A"));
        reloj.avanzar(Duration.ofMinutes(31));
        assertNull(service.bloqueadaHasta("tarjeta-A"));
        assertFalse(service.registrarIntento("tarjeta-A").bloqueado());
    }
}
