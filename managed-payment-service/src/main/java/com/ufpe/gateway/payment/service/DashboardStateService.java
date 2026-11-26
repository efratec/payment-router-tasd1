package com.ufpe.gateway.payment.service;

import com.ufpe.gateway.payment.config.LatencySimulatorConfig;
import com.ufpe.gateway.payment.domain.model.AcquirerType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DashboardStateService {

    private static final int MAX_RECORDS = 50;

    private final PaymentRoutingService routingService;
    private final LatencySimulatorConfig simulatorConfig;

    private final Deque<PaymentMetricRecord> recentPayments = new ConcurrentLinkedDeque<>();
    private final Deque<AdaptationRecord> adaptationHistory = new ConcurrentLinkedDeque<>();
    private final Deque<MapeKStatusRecord> mapekEvents = new ConcurrentLinkedDeque<>();
    private final Map<String, AtomicLong> countByAcquirer = new ConcurrentHashMap<>();

    private volatile MapeKStatusRecord latestMapeKStatus;

    public DashboardStateService(PaymentRoutingService routingService, LatencySimulatorConfig simulatorConfig) {
        this.routingService = routingService;
        this.simulatorConfig = simulatorConfig;
        String active = routingService.getActiveAcquirerType().name();
        String next = "STONE".equalsIgnoreCase(active) ? "CIELO" : "STONE";
        this.latestMapeKStatus = new MapeKStatusRecord(
                Instant.now(),
                active,
                null,
                next,
                3000L,
                "REDIS",
                0,
                2,
                "IDLE",
                "Aguardando telemetrias do analisador MAPE-K."
        );
    }

    public record PaymentMetricRecord(
            Instant timestamp,
            String transactionId,
            String acquirer,
            long latencyMs,
            boolean success,
            String errorCode
    ) {}

    public record AdaptationRecord(
            Instant timestamp,
            AcquirerType targetAcquirer,
            String reason
    ) {}

    public record MapeKStatusRecord(
            Instant timestamp,
            String trackedAcquirer,
            String metricAcquirer,
            String nextFailoverAcquirer,
            long latencyThresholdMs,
            String knowledgeBackend,
            int consecutiveViolations,
            int violationThreshold,
            String eventType,
            String message
    ) {}

    public record SimulatorStatus(
            boolean enabled,
            long delayMs
    ) {}

    public record DashboardSnapshot(
            String activeAcquirer,
            long totalPayments,
            long stoneCount,
            long cieloCount,
            Map<String, SimulatorStatus> simulators,
            MapeKStatusRecord mapekStatus,
            List<MapeKStatusRecord> mapekEvents,
            List<PaymentMetricRecord> recentPayments,
            List<AdaptationRecord> adaptationHistory
    ) {}

    public void recordPayment(String transactionId, String acquirer, long latencyMs, boolean success, String errorCode) {
        if (acquirer != null) {
            countByAcquirer.computeIfAbsent(acquirer.toUpperCase(), k -> new AtomicLong()).incrementAndGet();
        }
        recentPayments.addFirst(new PaymentMetricRecord(
                Instant.now(),
                transactionId,
                acquirer,
                latencyMs,
                success,
                errorCode
        ));
        while (recentPayments.size() > MAX_RECORDS) {
            recentPayments.removeLast();
        }
    }

    public void recordAdaptation(AcquirerType targetAcquirer, String reason) {
        adaptationHistory.addFirst(new AdaptationRecord(
                Instant.now(),
                targetAcquirer,
                reason
        ));
        while (adaptationHistory.size() > MAX_RECORDS) {
            adaptationHistory.removeLast();
        }
    }

    public void updateMapeKStatus(MapeKStatusRecord status) {
        String tracked = status.trackedAcquirer() != null
                ? status.trackedAcquirer().toUpperCase()
                : routingService.getActiveAcquirerType().name();
        String nextFailover = status.nextFailoverAcquirer() != null && !status.nextFailoverAcquirer().isBlank()
                ? status.nextFailoverAcquirer().toUpperCase()
                : ("STONE".equals(tracked) ? "CIELO" : "STONE");
        long threshold = status.latencyThresholdMs() > 0 ? status.latencyThresholdMs() : 3000L;
        String backend = status.knowledgeBackend() != null && !status.knowledgeBackend().isBlank()
                ? status.knowledgeBackend().toUpperCase()
                : "REDIS";

        MapeKStatusRecord record = new MapeKStatusRecord(
                status.timestamp() != null ? status.timestamp() : Instant.now(),
                tracked,
                status.metricAcquirer(),
                nextFailover,
                threshold,
                backend,
                status.consecutiveViolations(),
                status.violationThreshold() > 0 ? status.violationThreshold() : 2,
                status.eventType(),
                status.message()
        );
        this.latestMapeKStatus = record;
        mapekEvents.addFirst(record);
        while (mapekEvents.size() > MAX_RECORDS) {
            mapekEvents.removeLast();
        }
    }

    public DashboardSnapshot getSnapshot() {
        long stoneCount = countByAcquirer.getOrDefault("STONE", new AtomicLong(0)).get();
        long cieloCount = countByAcquirer.getOrDefault("CIELO", new AtomicLong(0)).get();

        Map<String, SimulatorStatus> simulators = Map.of(
                "STONE", new SimulatorStatus(
                        simulatorConfig.isSimulateLatency("STONE"),
                        simulatorConfig.getDelay("STONE")
                ),
                "CIELO", new SimulatorStatus(
                        simulatorConfig.isSimulateLatency("CIELO"),
                        simulatorConfig.getDelay("CIELO")
                )
        );

        return new DashboardSnapshot(
                routingService.getActiveAcquirerType().name(),
                stoneCount + cieloCount,
                stoneCount,
                cieloCount,
                simulators,
                latestMapeKStatus,
                new ArrayList<>(mapekEvents),
                new ArrayList<>(recentPayments),
                new ArrayList<>(adaptationHistory)
        );
    }

}