package com.califorge.mspago.controller;

import com.califorge.mspago.dto.ConfirmarPagoRequest;
import com.califorge.mspago.dto.IniciarPagoRequest;
import com.califorge.mspago.dto.IniciarPagoResponse;
import com.califorge.mspago.dto.PagoResponse;
import com.califorge.mspago.model.AlertaFraude;
import com.califorge.mspago.service.AuditoriaPagoService;
import com.califorge.mspago.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final AuditoriaPagoService auditoria;

    public PagoController(PagoService pagoService, AuditoriaPagoService auditoria) {
        this.pagoService = pagoService;
        this.auditoria = auditoria;
    }

    /** Inicia el pago. 429 si el antifraude bloquea la tarjeta. */
    @PostMapping("/iniciar")
    public ResponseEntity<IniciarPagoResponse> iniciar(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody IniciarPagoRequest req) {
        return ResponseEntity.status(201).body(pagoService.iniciar(jwt.getSubject(), req));
    }

    /** Confirma (commit) el pago con el token que WebPay devuelve al retornar al sitio. */
    @PostMapping("/confirmar")
    public PagoResponse confirmar(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody ConfirmarPagoRequest req) {
        return pagoService.confirmar(jwt.getSubject(), req);
    }

    @GetMapping("/mis-pagos")
    public List<PagoResponse> misPagos(@AuthenticationPrincipal Jwt jwt) {
        return pagoService.misPagos(jwt.getSubject());
    }

    /** Ultimas alertas de fraude (ADMINISTRADOR). */
    @GetMapping("/alertas")
    public List<AlertaFraude> alertas() {
        return auditoria.ultimasAlertas();
    }
}
