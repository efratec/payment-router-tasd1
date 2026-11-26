package com.ufpe.gateway.payment.domain.strategy;

import com.ufpe.gateway.payment.config.LatencySimulatorConfig;
import com.ufpe.gateway.payment.domain.model.PaymentRequest;
import com.ufpe.gateway.payment.domain.model.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component("PAGSEGURO")
@RequiredArgsConstructor
public class PagseguroPaymentStrategy implements PaymentStrategy {

    private static final String ACQUIRER_NAME = "PAGSEGURO";

    private final LatencySimulatorConfig simulatorConfig;

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        if (simulatorConfig.isSimulateLatency(ACQUIRER_NAME)) {
            long delay = simulatorConfig.getDelay(ACQUIRER_NAME);
            log.warn("⏳ [PAGSEGURO] Simulando latência de {}ms | transactionId={}", delay, request.getTransactionId());
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Execução interrompida durante simulação de latência na PAGSEGURO", e);
            }
        }

        log.info("💳 [PAGSEGURO] Processando pagamento | transactionId={} | amount={}",
                request.getTransactionId(), request.getAmount());

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
