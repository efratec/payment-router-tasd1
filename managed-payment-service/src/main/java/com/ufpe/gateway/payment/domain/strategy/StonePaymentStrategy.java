package com.ufpe.gateway.payment.domain.strategy;

import com.ufpe.gateway.payment.config.LatencySimulatorConfig;
import com.ufpe.gateway.payment.domain.model.PaymentRequest;
import com.ufpe.gateway.payment.domain.model.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component("STONE")
@RequiredArgsConstructor
public class StonePaymentStrategy implements PaymentStrategy {

    private static final String ACQUIRER_NAME = "STONE";

    private final LatencySimulatorConfig simulatorConfig;

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        if (simulatorConfig.isSimulateLatency(ACQUIRER_NAME)) {
            long delay = simulatorConfig.getDelay(ACQUIRER_NAME);
            try {
                log.warn("⏳ [SIMULADOR] Latência alta em [STONE] (Virtual Thread liberada). Aguardando {} ms...", delay);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrompida durante simulação de latência na STONE", e);
            }
        }

        log.info("💳 Processando pagamento via [STONE] para ID: {}", request.getTransactionId());
        return new PaymentResult(
                request.getTransactionId(),
                ACQUIRER_NAME,
                true,
                "APPROVED-" + UUID.randomUUID().toString().substring(0, 8)
        );
    }

    @Override
    public String getAcquirerName() {
        return ACQUIRER_NAME;
    }

}