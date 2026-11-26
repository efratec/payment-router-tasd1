package com.ufpe.gateway.mapek.monitor;

import com.ufpe.gateway.mapek.analysis.MapeKAnalyzer;
import com.ufpe.gateway.mapek.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryConsumer {

    private final MapeKAnalyzer analyzer;

    @RabbitListener(queues = RabbitMQConfig.TELEMETRY_QUEUE)
    public void onTelemetryReceived(TelemetryEvent event) {
        log.info("📥 [MONITOR] Telemetria recebida | acquirer={} | latencyMs={} | success={}",
                event.acquirer(), event.latencyMs(), event.success());
        analyzer.analyze(event);
    }

}