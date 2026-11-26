package com.ufpe.gateway.mapek.command;

import com.ufpe.gateway.mapek.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void sendRouteChangeCommand(String targetAcquirer, String reason) {
        var command = new AdaptationCommandDTO(targetAcquirer, reason);
        log.info("📤 [EXECUTE] Publicando comando de adaptação | targetAcquirer={} | reason={}",
                targetAcquirer, reason);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.COMMAND_EXCHANGE,
                RabbitMQConfig.COMMAND_ROUTING_KEY,
                command
        );
    }

}