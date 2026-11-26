package com.ufpe.gateway.payment.service;

import com.ufpe.gateway.payment.domain.model.AcquirerType;
import com.ufpe.gateway.payment.domain.strategy.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRoutingService {

    private final Map<String, PaymentStrategy> strategies;
    private final AtomicReference<AcquirerType> activeAcquirer = new AtomicReference<>(AcquirerType.STONE);

    public PaymentStrategy getActiveStrategy() {
        String acquirerName = activeAcquirer.get().name();
        PaymentStrategy strategy = strategies.get(acquirerName);
        if (strategy == null) {
            throw new IllegalStateException("Nenhuma PaymentStrategy registrada para a adquirente: " + acquirerName);
        }
        return strategy;
    }

    public synchronized void updateActiveRoute(AcquirerType newAcquirer) {
        log.info("🔄 [EFETOR] Mudança de rota aplicada em memória. Nova adquirente ativa: {}", newAcquirer);
        this.activeAcquirer.set(newAcquirer);
    }

    public AcquirerType getActiveAcquirerType() {
        return activeAcquirer.get();
    }

}