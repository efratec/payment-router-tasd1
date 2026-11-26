package com.ufpe.gateway.payment.effectors;

import com.ufpe.gateway.payment.config.RabbitMQConfig;
import com.ufpe.gateway.payment.service.DashboardStateService;
import com.ufpe.gateway.payment.service.PaymentRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandConsumer {

    private final PaymentRoutingService routingService;
    private final DashboardStateService dashboardStateService;

    @RabbitListener(queues = RabbitMQConfig.COMMAND_QUEUE)
    public void consumeCommand(AdaptationCommandDTO command) {
        if (command == null || command.targetAcquirer() == null) {
            log.warn("⚠️ [COMMAND] Comando de adaptação inválido ou vazio recebido.");
            return;
        }

        log.info("📥 [COMMAND] Comando recebido para alternar rota para {} (Motivo: {})",
                command.targetAcquirer(), command.reason());

        routingService.updateActiveRoute(command.targetAcquirer());
        dashboardStateService.recordAdaptation(command.targetAcquirer(), command.reason());
    }

}