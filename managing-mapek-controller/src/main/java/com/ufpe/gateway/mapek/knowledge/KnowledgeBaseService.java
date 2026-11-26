package com.ufpe.gateway.mapek.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ufpe.gateway.mapek.knowledge.model.SloTarget;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class KnowledgeBaseService {

    // Prefixo para as chaves no Redis
    private static final String SLO_KEY_PREFIX = "mapek:knowledge:slo:";
    private static final String COOLDOWN_KEY_PREFIX = "mapek:knowledge:cooldown:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgeBaseService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        // Inicializa dados padrão no Redis caso não existam
        initializeDefaultSlos();
    }

    private void initializeDefaultSlos() {
        saveSlo(new SloTarget("STONE", 1000L, 0.15));
        saveSlo(new SloTarget("CIELO", 1200L, 0.20));
    }

    public void saveSlo(SloTarget slo) {
        try {
            String json = objectMapper.writeValueAsString(slo);
            redisTemplate.opsForValue().set(SLO_KEY_PREFIX + slo.acquirerName(), json);
        } catch (Exception e) {
            System.err.println("❌ Erro ao salvar SLO no Redis: " + e.getMessage());
        }
    }

    public SloTarget getSlo(String acquirer) {
        try {
            String json = redisTemplate.opsForValue().get(SLO_KEY_PREFIX + acquirer);
            if (json != null) {
                return objectMapper.readValue(json, SloTarget.class);
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao ler SLO do Redis: " + e.getMessage());
        }
        // Fallback padrão caso o Redis falhe ou não encontre
        return new SloTarget(acquirer, 1000L, 0.15);
    }

    // --- Controle de Cooldown para evitar Flapping ---

    public boolean isInCooldown(String acquirer) {
        String key = COOLDOWN_KEY_PREFIX + acquirer;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setCooldown(String acquirer, long timeoutMinutes) {
        String key = COOLDOWN_KEY_PREFIX + acquirer;
        // O Redis expira a chave sozinho após o tempo de cooldown (ex: 5 minutos)
        redisTemplate.opsForValue().set(key, "ACTIVE", Duration.ofMinutes(timeoutMinutes));
    }
}