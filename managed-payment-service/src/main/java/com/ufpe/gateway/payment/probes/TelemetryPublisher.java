package com.ufpe.gateway.payment.probes;

import com.ufpe.gateway.payment.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryPublisher {

    private final RabbitTemplate rabbitTemplate;

    public record TelemetryEvent(
            String acquirer,
            long latencyMs,
            boolean success,
            String errorCode
    ) {}

    @Async
    public void publishMetricAsync(String acquirer, long latencyMs, boolean success, String errorCode) {
        TelemetryEvent event = new TelemetryEvent(acquirer, latencyMs, success, errorCode);
        log.info("📡 [TELEMETRY_PUB] Publicando evento | acquirer={} | latencyMs={} | success={} | errorCode={}",
                acquirer, latencyMs, success, errorCode);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TELEMETRY_EXCHANGE,
                "telemetry.key",
                event
        );
    }

}