package com.ufpe.gateway.payment.service;

import com.ufpe.gateway.payment.domain.model.PaymentRequest;
import com.ufpe.gateway.payment.domain.model.PaymentResult;
import com.ufpe.gateway.payment.domain.strategy.PaymentStrategy;
import com.ufpe.gateway.payment.probes.TelemetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExecutionService {

    private final PaymentRoutingService routingService;
    private final TelemetryPublisher telemetryPublisher;
    private final DashboardStateService dashboardStateService;

    public PaymentResult executePayment(PaymentRequest request) {
        long startTime = System.currentTimeMillis();
        PaymentStrategy activeStrategy = routingService.getActiveStrategy();
        String acquirer = activeStrategy.getAcquirerName();

        log.debug("⚙️ [EXECUTION] Iniciando execução | transactionId={} | acquirer={}",
                request.getTransactionId(), acquirer);

        try {
            PaymentResult result = activeStrategy.processPayment(request);
            long latency = System.currentTimeMillis() - startTime;

            log.info("📊 [PROBE] Telemetria registrada (sucesso) | transactionId={} | acquirer={} | latencyMs={}",
                    request.getTransactionId(), acquirer, latency);

            dashboardStateService.recordPayment(request.getTransactionId(), acquirer, latency, result.success(), null);
            telemetryPublisher.publishMetricAsync(acquirer, latency, result.success(), null);
            return result;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            String errorCode = e.getClass().getSimpleName();

            log.warn("📊 [PROBE] Telemetria registrada (falha) | transactionId={} | acquirer={} | latencyMs={} | error={}",
                    request.getTransactionId(), acquirer, latency, errorCode);

            dashboardStateService.recordPayment(request.getTransactionId(), acquirer, latency, false, errorCode);
            telemetryPublisher.publishMetricAsync(acquirer, latency, false, errorCode);
            throw e;
        }
    }

}