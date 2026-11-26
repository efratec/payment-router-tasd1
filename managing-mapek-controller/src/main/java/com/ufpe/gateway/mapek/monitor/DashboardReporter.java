package com.ufpe.gateway.mapek.monitor; // ajuste para o pacote adequado no controller

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class DashboardReporter {

    private final RestClient restClient;

    public DashboardReporter(
            @Value("${dashboard.reporter.url:http://localhost:8080/api/dashboard/mapek-status}") String url) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(300));
        factory.setReadTimeout(Duration.ofMillis(500));
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    public record MapeKStatusPayload(
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

    public void report(
            String trackedAcquirer,
            String metricAcquirer,
            int consecutiveViolations,
            int violationThreshold,
            String eventType,
            String message
    ) {
        String normalizedTracked = trackedAcquirer != null ? trackedAcquirer.toUpperCase() : "STONE";
        String nextFailover = "STONE".equals(normalizedTracked) ? "CIELO" : "STONE";
        report(
                normalizedTracked,
                metricAcquirer,
                nextFailover,
                3000L,
                "REDIS",
                consecutiveViolations,
                violationThreshold,
                eventType,
                message
        );
    }

    public void report(
            String trackedAcquirer,
            String metricAcquirer,
            String nextFailoverAcquirer,
            long latencyThresholdMs,
            String knowledgeBackend,
            int consecutiveViolations,
            int violationThreshold,
            String eventType,
            String message
    ) {
        try {
            MapeKStatusPayload payload = new MapeKStatusPayload(
                    Instant.now(),
                    trackedAcquirer,
                    metricAcquirer,
                    nextFailoverAcquirer,
                    latencyThresholdMs,
                    knowledgeBackend,
                    consecutiveViolations,
                    violationThreshold,
                    eventType,
                    message
            );
            restClient.post()
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.debug("Não foi possível reportar status ao dashboard: {}", e.getMessage());
        }
    }

}