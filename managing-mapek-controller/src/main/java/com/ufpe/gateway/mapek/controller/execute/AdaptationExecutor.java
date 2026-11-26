package com.ufpe.gateway.mapek.controller.execute;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdaptationExecutor {

    private final RabbitTemplate rabbitTemplate;

    public void executeAdaptation(String targetAcquirer, String reason) {
        Map<String, String> commandPayload = Map.of(
                "targetAcquirer", targetAcquirer,
                "reason", reason
        );

        System.out.println("🚀 [EXECUTOR] Publicando comando de adaptação no RabbitMQ -> Nova Rota: " + targetAcquirer);

        // Publica na fila de comandos que o managed-payment-service está ouvindo
        rabbitTemplate.convertAndSend("payment.commands.exchange", "command.key", commandPayload);
    }
}