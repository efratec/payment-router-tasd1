package com.ufpe.gateway.mapek.knowledge;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class RedisKnowledgeStore {

    private final StringRedisTemplate redisTemplate;
    private final String trackedAcquirerKey;
    private final String degradedCountKey;
    private final String lastReasonKey;
    private final String defaultAcquirer;

    // Fallback em memória caso o Redis esteja fora do ar
    private final AtomicReference<String> localTrackedAcquirer;
    private final AtomicInteger localDegradedCount = new AtomicInteger(0);
    private final AtomicReference<String> localLastReason = new AtomicReference<>("");

    public RedisKnowledgeStore(StringRedisTemplate redisTemplate,
                               @Value("${mapek.knowledge.key-prefix:mapek:knowledge:}") String prefix,
                               @Value("${mapek.knowledge.default-acquirer:STONE}") String defaultAcquirer) {
        this.redisTemplate = redisTemplate;
        this.trackedAcquirerKey = prefix + "tracked-acquirer";
        this.degradedCountKey = prefix + "degraded-count";
        this.lastReasonKey = prefix + "last-reason";
        this.defaultAcquirer = defaultAcquirer.toUpperCase();
        this.localTrackedAcquirer = new AtomicReference<>(this.defaultAcquirer);
    }

    @PostConstruct
    public void init() {
        try {
            String storedAcquirer = redisTemplate.opsForValue().get(trackedAcquirerKey);
            if (storedAcquirer != null && !storedAcquirer.isBlank()) {
                localTrackedAcquirer.set(storedAcquirer.toUpperCase());
            } else {
                redisTemplate.opsForValue().set(trackedAcquirerKey, defaultAcquirer);
                localTrackedAcquirer.set(defaultAcquirer);
            }

            String storedCount = redisTemplate.opsForValue().get(degradedCountKey);
            if (storedCount != null) {
                localDegradedCount.set(Integer.parseInt(storedCount));
            } else {
                redisTemplate.opsForValue().set(degradedCountKey, "0");
                localDegradedCount.set(0);
            }

            String storedReason = redisTemplate.opsForValue().get(lastReasonKey);
            if (storedReason != null) {
                localLastReason.set(storedReason);
            }

            log.info("🧠 [KNOWLEDGE/REDIS] Carregado do Redis | trackedAcquirer={} | degradedCount={}",
                    localTrackedAcquirer.get(), localDegradedCount.get());
        } catch (Exception e) {
            log.warn("⚠️ [KNOWLEDGE/REDIS] Falha ao ler estado inicial no Redis, usando fallback em memória: {}",
                    e.getMessage());
        }
    }

    public String getTrackedAcquirer() {
        try {
            String val = redisTemplate.opsForValue().get(trackedAcquirerKey);
            if (val != null && !val.isBlank()) {
                String normalized = val.toUpperCase();
                localTrackedAcquirer.set(normalized);
                return normalized;
            }
        } catch (Exception e) {
            log.debug("[KNOWLEDGE/REDIS] Fallback getTrackedAcquirer: {}", e.getMessage());
        }
        return localTrackedAcquirer.get();
    }

    public void setTrackedAcquirer(String acquirer) {
        String normalized = acquirer.toUpperCase();
        localTrackedAcquirer.set(normalized);
        try {
            redisTemplate.opsForValue().set(trackedAcquirerKey, normalized);
        } catch (Exception e) {
            log.warn("⚠️ [KNOWLEDGE/REDIS] Falha ao salvar trackedAcquirer no Redis: {}", e.getMessage());
        }
    }

    public int getDegradedCount() {
        try {
            String val = redisTemplate.opsForValue().get(degradedCountKey);
            if (val != null) {
                int parsed = Integer.parseInt(val);
                localDegradedCount.set(parsed);
                return parsed;
            }
        } catch (Exception e) {
            log.debug("[KNOWLEDGE/REDIS] Fallback getDegradedCount: {}", e.getMessage());
        }
        return localDegradedCount.get();
    }

    public int incrementDegradedCount() {
        try {
            Long next = redisTemplate.opsForValue().increment(degradedCountKey);
            if (next != null) {
                localDegradedCount.set(next.intValue());
                return next.intValue();
            }
        } catch (Exception e) {
            log.warn("⚠️ [KNOWLEDGE/REDIS] Falha ao incrementar degradedCount no Redis: {}", e.getMessage());
        }
        return localDegradedCount.incrementAndGet();
    }

    public void resetDegradedCount() {
        localDegradedCount.set(0);
        try {
            redisTemplate.opsForValue().set(degradedCountKey, "0");
        } catch (Exception e) {
            log.warn("⚠️ [KNOWLEDGE/REDIS] Falha ao resetar degradedCount no Redis: {}", e.getMessage());
        }
    }

    public void setLastReason(String reason) {
        String safeReason = reason != null ? reason : "";
        localLastReason.set(safeReason);
        try {
            redisTemplate.opsForValue().set(lastReasonKey, safeReason);
        } catch (Exception e) {
            log.debug("[KNOWLEDGE/REDIS] Falha ao salvar lastReason no Redis: {}", e.getMessage());
        }
    }

}