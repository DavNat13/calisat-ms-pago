package com.califorge.mspago.service;

import com.califorge.mspago.dto.AlertaFraudeEvent;
import com.califorge.mspago.dto.ConfirmarPagoRequest;
import com.califorge.mspago.dto.IniciarPagoRequest;
import com.califorge.mspago.dto.IniciarPagoResponse;
import com.califorge.mspago.dto.IntentoPagoEvent;
import com.califorge.mspago.dto.PagoResponse;
import com.califorge.mspago.model.EstadoPago;
import com.califorge.mspago.model.Pago;
import com.califorge.mspago.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Orquesta el pago: antifraude (velocity check), publicacion a Kafka y pasarela WebPay. */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagos;
    private final VelocityCheckService velocity;
    private final WebpayGateway gateway;
    private final KafkaTemplate<String, Object> kafka;
    private final Clock clock;
    private final String topicIntentos;
    private final String topicAlertas;
    private final String returnUrl;

    public PagoService(PagoRepository pagos, VelocityCheckService velocity, WebpayGateway gateway,
                       KafkaTemplate<String, Object> kafka, Clock clock,
                       @Value("${calisat.pago.kafka.topic-intentos}") String topicIntentos,
                       @Value("${calisat.pago.kafka.topic-alertas}") String topicAlertas,
                       @Value("${calisat.pago.webpay.return-url}") String returnUrl) {
        this.pagos = pagos;
        this.velocity = velocity;
        this.gateway = gateway;
        this.kafka = kafka;
        this.clock = clock;
        this.topicIntentos = topicIntentos;
        this.topicAlertas = topicAlertas;
        this.returnUrl = returnUrl;
    }

    /** noRollbackFor: el bloqueo y el pago BLOQUEADO deben persistir aunque se responda 429. */
    @Transactional(noRollbackFor = TarjetaBloqueadaException.class)
    public IniciarPagoResponse iniciar(String clienteSub, IniciarPagoRequest req) {
        String hash = TarjetaUtil.hash(req.tarjeta());
        String ult4 = TarjetaUtil.ultimos4(req.tarjeta());
        String idTransaccion = UUID.randomUUID().toString();

        Instant bloqueadaHasta = velocity.bloqueadaHasta(hash);
        if (bloqueadaHasta != null) {
            publicarIntento(idTransaccion, hash, ult4, clienteSub, req, true);
            throw new TarjetaBloqueadaException(bloqueadaHasta);
        }

        VelocityCheckService.Resultado r = velocity.registrarIntento(hash);
        publicarIntento(idTransaccion, hash, ult4, clienteSub, req, r.bloqueado());
        if (r.bloqueado()) {
            publicarAlerta(new AlertaFraudeEvent(hash, ult4, clienteSub, r.intentosEnVentana(),
                    velocity.ventanaSegundos(), r.bloqueadoHasta(), clock.instant()));
            Pago bloqueado = nuevoPago(clienteSub, req, hash, ult4);
            bloqueado.setEstado(EstadoPago.BLOQUEADO);
            pagos.save(bloqueado);
            throw new TarjetaBloqueadaException(r.bloqueadoHasta());
        }

        Pago pago = nuevoPago(clienteSub, req, hash, ult4);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setSimulado(gateway.esSimulado());
        pagos.save(pago);
        WebpayGateway.TransaccionCreada tx = gateway.crear(pago.getBuyOrder(), clienteSub,
                req.monto().doubleValue(), returnUrl);
        pago.setToken(tx.token());
        pagos.save(pago);
        log.info("Pago {} iniciado (orden={}, simulado={})", pago.getId(), req.ordenId(), pago.isSimulado());
        return new IniciarPagoResponse(pago.getId(), tx.token(), tx.url(), pago.isSimulado());
    }

    @Transactional
    public PagoResponse confirmar(String clienteSub, ConfirmarPagoRequest req) {
        Pago pago = pagos.findByToken(req.token())
                .filter(p -> p.getClienteSub().equals(clienteSub))
                .orElseThrow(() -> new PagoNoEncontradoException("Pago no encontrado para el token informado"));
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            return PagoResponse.de(pago);
        }
        WebpayGateway.ResultadoCommit res = gateway.confirmar(req.token());
        pago.setEstado(res.autorizado() ? EstadoPago.PAGADO : EstadoPago.RECHAZADO);
        pago.setCodigoAutorizacion(res.codigoAutorizacion());
        pagos.save(pago);
        if (res.autorizado() && pago.getTarjetaHash() != null) {
            velocity.limpiar(pago.getTarjetaHash());
        }
        log.info("Pago {} confirmado: {}", pago.getId(), pago.getEstado());
        return PagoResponse.de(pago);
    }

    public List<PagoResponse> misPagos(String clienteSub) {
        return pagos.findByClienteSubOrderByCreadoDesc(clienteSub).stream().map(PagoResponse::de).toList();
    }

    private Pago nuevoPago(String clienteSub, IniciarPagoRequest req, String hash, String ult4) {
        Pago p = new Pago();
        p.setOrdenId(req.ordenId());
        p.setClienteSub(clienteSub);
        p.setMonto(req.monto());
        p.setTarjetaHash(hash);
        p.setTarjetaUltimos4(ult4);
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String ordenCorta = req.ordenId().replace("-", "");
        ordenCorta = ordenCorta.length() > 12 ? ordenCorta.substring(0, 12) : ordenCorta;
        String buyOrder = "CAL" + ordenCorta + "-" + sufijo;
        p.setBuyOrder(buyOrder.length() > 26 ? buyOrder.substring(0, 26) : buyOrder);
        return p;
    }

    private void publicarIntento(String idTx, String hash, String ult4, String sub, IniciarPagoRequest req,
                                 boolean bloqueado) {
        try {
            kafka.send(topicIntentos, hash, new IntentoPagoEvent(idTx, hash, ult4, sub, req.ordenId(),
                    req.monto(), bloqueado, clock.instant()));
        } catch (RuntimeException e) {
            log.error("No se pudo publicar intento de pago en Kafka (flujo continuado): {}", e.getMessage());
        }
    }

    private void publicarAlerta(AlertaFraudeEvent evento) {
        try {
            kafka.send(topicAlertas, evento.tarjetaHash(), evento);
        } catch (RuntimeException e) {
            log.error("No se pudo publicar alerta de fraude en Kafka: {}", e.getMessage());
        }
    }
}
