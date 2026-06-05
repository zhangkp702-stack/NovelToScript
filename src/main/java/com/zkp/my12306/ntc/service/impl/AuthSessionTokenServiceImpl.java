package com.zkp.my12306.ntc.service.impl;

import com.zkp.my12306.ntc.service.AuthSessionTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSessionTokenServiceImpl implements AuthSessionTokenService {
    private static final String SESSION_KEY_PREFIX = "ntc:session:";

    private final StringRedisTemplate redisTemplate;
    private final boolean useRedis;
    private final long ttlSeconds;
    private final Map<String, LocalSession> localSessionStore = new ConcurrentHashMap<>();

    public AuthSessionTokenServiceImpl(
            StringRedisTemplate redisTemplate,
            @Value("${auth.session.use-redis:true}") boolean useRedis,
            @Value("${auth.session.ttl-seconds:7200}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.useRedis = useRedis;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String createSessionToken(String username) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        if (useRedis) {
            redisTemplate.opsForValue().set(redisKey(sessionId), username, Duration.ofSeconds(ttlSeconds));
        } else {
            localSessionStore.put(sessionId, new LocalSession(username, Instant.now().plusSeconds(ttlSeconds)));
        }
        return sessionId;
    }

    @Override
    public boolean isValid(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        if (useRedis) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey(sessionId)));
        }
        LocalSession localSession = localSessionStore.get(sessionId);
        if (localSession == null) {
            return false;
        }
        if (localSession.expiresAt().isBefore(Instant.now())) {
            localSessionStore.remove(sessionId);
            return false;
        }
        return true;
    }

    @Override
    public String getUsername(String sessionId) {
        if (!isValid(sessionId)) {
            return null;
        }
        if (useRedis) {
            return redisTemplate.opsForValue().get(redisKey(sessionId));
        }
        LocalSession localSession = localSessionStore.get(sessionId);
        return localSession == null ? null : localSession.username();
    }

    @Override
    public void refreshTtl(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        if (useRedis) {
            String username = redisTemplate.opsForValue().get(redisKey(sessionId));
            if (username != null && !username.isBlank()) {
                redisTemplate.opsForValue().set(redisKey(sessionId), username, Duration.ofSeconds(ttlSeconds));
            }
            return;
        }
        LocalSession localSession = localSessionStore.get(sessionId);
        if (localSession != null) {
            localSessionStore.put(sessionId, new LocalSession(localSession.username(), Instant.now().plusSeconds(ttlSeconds)));
        }
    }

    @Override
    public void revoke(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        if (useRedis) {
            redisTemplate.delete(redisKey(sessionId));
        } else {
            localSessionStore.remove(sessionId);
        }
    }

    private String redisKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private record LocalSession(String username, Instant expiresAt) {
    }
}
