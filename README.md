# calisat-ms-pago

Pagos WebPay + antifraude (Velocity Check) — puerto **8089** · Java 21 · Spring Boot 4.1.0

## Endpoints (JWT Entra/Cognito)
```
POST /api/v1/pagos/iniciar {ordenId,monto,tarjeta} → 201 {token,url} | 429 si la tarjeta está bloqueada
POST /api/v1/pagos/confirmar {token} · GET /api/v1/pagos/mis-pagos · GET /api/v1/pagos/alertas (ADMIN)
Antifraude: >3 intentos de la misma tarjeta en 10 s → bloqueo 30 min, alerta en `payment-alerts` y 429. La tarjeta se guarda solo como SHA-256 + últimos 4.
```

## Variables de entorno
SPRING_DATASOURCE_*, SPRING_KAFKA_BOOTSTRAP_SERVERS, WEBPAY_ENABLED (true=Transbank integración, false=simulación), WEBPAY_RETURN_URL

## Ejecutar
```
./mvnw clean test
docker compose up -d --build   # requiere calisat-infra arriba (red calisat-net)
```
