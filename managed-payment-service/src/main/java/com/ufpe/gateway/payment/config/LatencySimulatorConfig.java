package com.ufpe.gateway.payment.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LatencySimulatorConfig {

    private record SimulationState(boolean enabled, long delayMs) {}

    private final Map<String, SimulationState> statesByAcquirer = new ConcurrentHashMap<>();

    public void setSimulateLatency(boolean enable, long delay) {
        setSimulateLatency("STONE", enable, delay);
    }

    public void setSimulateLatency(String acquirer, boolean enable, long delay) {
        if (acquirer == null || acquirer.isBlank()) {
            return;
        }
        String key = acquirer.trim().toUpperCase();
        if (enable) {
            statesByAcquirer.put(key, new SimulationState(true, Math.max(0L, delay)));
        } else {
            statesByAcquirer.remove(key);
        }
    }

    public boolean isSimulateLatency(String acquirer) {
        if (acquirer == null) {
            return false;
        }
        SimulationState state = statesByAcquirer.get(acquirer.trim().toUpperCase());
        return state != null && state.enabled();
    }

    public long getDelay(String acquirer) {
        if (acquirer == null) {
            return 0L;
        }
        SimulationState state = statesByAcquirer.get(acquirer.trim().toUpperCase());
        return state != null ? state.delayMs() : 0L;
    }

}