package com.ufpe.gateway.payment.telemetry;

import com.ufpe.gateway.payment.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishTelemetry(TelemetryMessage message) {
        log.info("📡 [TELEMETRY] Enviando métrica: acquirer={}, durationMs={}, success={}",
                message.acquirer(), message.durationMs(), message.success());
        rabbitTemplate.convertAndSend(RabbitMQConfig.TELEMETRY_EXCHANGE, "telemetry.key", message);
    }
    
}