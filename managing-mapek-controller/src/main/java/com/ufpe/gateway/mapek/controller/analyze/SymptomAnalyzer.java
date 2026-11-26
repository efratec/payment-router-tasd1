package com.ufpe.gateway.mapek.controller.analyze;

import com.ufpe.gateway.mapek.monitor.TelemetryMessage;
import com.ufpe.gateway.mapek.controller.plan.FallbackPlanner;
import com.ufpe.gateway.mapek.knowledge.KnowledgeBaseService;
import com.ufpe.gateway.mapek.knowledge.model.SloTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SymptomAnalyzer {

    private final KnowledgeBaseService knowledgeBase;
    private final FallbackPlanner fallbackPlanner;

    public void analyzeSymptom(TelemetryMessage telemetry) {
        SloTarget slo = knowledgeBase.getSlo(telemetry.acquirer());

        boolean violatesLatency = telemetry.latencyMs() > slo.maxLatencyMs();
        boolean violatesSuccess = !telemetry.success();

        if (violatesLatency || violatesSuccess) {
            System.out.println("⚠️ [ANALISADOR] Violação de SLO detectada para " + telemetry.acquirer() + "!");

            // Aciona o Planejador gerando uma solicitação de mudança
            fallbackPlanner.planAdaptation(telemetry.acquirer(), violatesLatency ? "HIGH_LATENCY" : "TRANSACTION_FAILURE");
        }

    }

}
