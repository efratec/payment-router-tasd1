package com.ufpe.gateway.payment.controller;

import com.ufpe.gateway.payment.config.LatencySimulatorConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
public class SimulatorController {

    private final LatencySimulatorConfig simulatorConfig;

    @PostMapping("/latency")
    public String toggleLatency(
            @RequestParam(defaultValue = "STONE") String acquirer,
            @RequestParam boolean enable,
            @RequestParam(defaultValue = "5000") long delay) {

        String normalizedAcquirer = acquirer.trim().toUpperCase();
        simulatorConfig.setSimulateLatency(normalizedAcquirer, enable, delay);

        log.warn("🎛️ [SIMULATOR] acquirer={} | enable={} | delay={}ms",
                normalizedAcquirer, enable, delay);

        return "Simulação de latência da " + normalizedAcquirer + " no managed-payment-service: "
                + (enable ? "ATIVADA (" + delay + "ms)" : "DESATIVADA");
    }

}