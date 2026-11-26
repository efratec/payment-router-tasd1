package com.ufpe.gateway.mapek.controller.plan;

import com.ufpe.gateway.mapek.controller.execute.AdaptationExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FallbackPlanner {

    private final AdaptationExecutor adaptationExecutor;

    public void planAdaptation(String failingAcquirer, String reason) {
        // Lógica ECA simples: Se a Stone falhou, planeja migrar para a Cielo
        String targetAcquirer = failingAcquirer.equals("STONE") ? "CIELO" : "STONE";

        System.out.println("📋 [PLANEJADOR] Plano gerado: Desviar tráfego de " + failingAcquirer + " para " + targetAcquirer + " devido a " + reason);

        // Envia o plano para o Executor
        adaptationExecutor.executeAdaptation(targetAcquirer, reason);
    }

}
