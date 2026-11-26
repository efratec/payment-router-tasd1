package com.ufpe.gateway.mapek.analysis;

import com.ufpe.gateway.mapek.command.CommandPublisher;
import com.ufpe.gateway.mapek.knowledge.RedisKnowledgeStore;
import com.ufpe.gateway.mapek.monitor.DashboardReporter;
import com.ufpe.gateway.mapek.monitor.TelemetryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MapeKAnalyzer {

    private static final long LATENCY_THRESHOLD_MS = 3000L;
    private static final int CONSECUTIVE_VIOLATIONS_LIMIT = 2;

    private final CommandPublisher commandPublisher;
    private final DashboardReporter dashboardReporter;
    private final RedisKnowledgeStore knowledgeStore;

    public synchronized void analyze(TelemetryEvent event) {
        if (event == null || event.acquirer() == null) {
            log.warn("⚠️ [ANALYZE] Evento de telemetria inválido recebido.");
            return;
        }

        String acquirer = event.acquirer().toUpperCase();
        boolean degraded = !event.success() || event.latencyMs() > LATENCY_THRESHOLD_MS;

        log.info("🔍 [ANALYZE] acquirer={} | latencyMs={} | success={} | errorCode={} | degraded={}",
                acquirer, event.latencyMs(), event.success(), event.errorCode(), degraded);

        String tracked = knowledgeStore.getTrackedAcquirer();

        // Só avalia failover quando a métrica pertence à adquirente monitorada como ativa
        if (!acquirer.equals(tracked)) {
            String msg = String.format("Ignorando métrica de %s pois rota rastreada atual é %s", acquirer, tracked);
            log.warn("ℹ️ [KNOWLEDGE] {}", msg);
            dashboardReporter.report(
                    tracked,
                    acquirer,
                    knowledgeStore.getDegradedCount(),
                    CONSECUTIVE_VIOLATIONS_LIMIT,
                    "IGNORED_MISMATCH",
                    msg
            );
            return;
        }

        if (degraded) {
            int count = knowledgeStore.incrementDegradedCount();
            log.warn("⚠️ [ANALYZE] Degradação detectada em {} | violacoesConsecutivas={}/{}",
                    acquirer, count, CONSECUTIVE_VIOLATIONS_LIMIT);

            String msg = String.format(
                    "Degradação em %s (%d/%d) | latencyMs=%d, success=%s, error=%s",
                    acquirer, count, CONSECUTIVE_VIOLATIONS_LIMIT,
                    event.latencyMs(), event.success(), event.errorCode()
            );
            knowledgeStore.setLastReason(msg);
            dashboardReporter.report(
                    tracked,
                    acquirer,
                    count,
                    CONSECUTIVE_VIOLATIONS_LIMIT,
                    "VIOLATION",
                    msg
            );

            if (count >= CONSECUTIVE_VIOLATIONS_LIMIT) {
                planAndExecuteFailover(acquirer, event);
            }
        } else {
            if (knowledgeStore.getDegradedCount() > 0) {
                log.info("✅ [ANALYZE] Métrica saudável em {}. Resetando contador de degradação.", acquirer);
                knowledgeStore.resetDegradedCount();
            }
            String msg = String.format("Métrica saudável em %s (latencyMs=%d)", acquirer, event.latencyMs());
            dashboardReporter.report(
                    tracked,
                    acquirer,
                    0,
                    CONSECUTIVE_VIOLATIONS_LIMIT,
                    "HEALTHY",
                    msg
            );
        }
    }

    private void planAndExecuteFailover(String currentAcquirer, TelemetryEvent lastEvent) {
        String nextAcquirer = "STONE".equals(currentAcquirer) ? "CIELO" : "STONE";
        String reason = String.format(
                "Latência/falha em %s (latencyMs=%d, success=%s, error=%s)",
                currentAcquirer, lastEvent.latencyMs(), lastEvent.success(), lastEvent.errorCode()
        );

        log.info("🧠 [PLAN] Limiar atingido. Planejando failover de {} para {}", currentAcquirer, nextAcquirer);

        // Atualiza Knowledge no Redis e reseta contador antes de emitir o comando
        knowledgeStore.setTrackedAcquirer(nextAcquirer);
        knowledgeStore.resetDegradedCount();
        knowledgeStore.setLastReason(reason);

        String msg = String.format("Failover de %s para %s (%s)", currentAcquirer, nextAcquirer, reason);
        dashboardReporter.report(
                nextAcquirer,
                currentAcquirer,
                0,
                CONSECUTIVE_VIOLATIONS_LIMIT,
                "FAILOVER",
                msg
        );

        commandPublisher.sendRouteChangeCommand(nextAcquirer, reason);
    }

}