package com.clas.common;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeStore {
    private static final long TTL_SECONDS = 10 * 60;
    private static final long COOLDOWN_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final String fixedCode;
    private final Map<String, CodeEntry> fallbackStore = new ConcurrentHashMap<>();

    public VerificationCodeStore(
        StringRedisTemplate redisTemplate,
        @Value("${clas.verification.fixed-code:}") String fixedCode
    ) {
        this.redisTemplate = redisTemplate;
        this.fixedCode = fixedCode == null ? "" : fixedCode.trim();
    }

    public String generateAndStore(String phone, String scene) {
        String normalizedPhone = PhoneValidator.normalizeAndValidate(phone);
        String normalizedScene = normalizeScene(scene);
        String code = fixedCode.isBlank()
            ? String.format("%06d", (int) (Math.random() * 1000000))
            : fixedCode;

        try {
            storeInRedis(normalizedPhone, normalizedScene, code);
        } catch (RedisConnectionFailureException | IllegalStateException ex) {
            storeInMemory(normalizedPhone, normalizedScene, code);
        }

        System.out.println("[VerificationCode] 场景 " + normalizedScene + "，手机号 " + maskPhone(normalizedPhone) + " 的验证码: " + code);
        return code;
    }

    public boolean verify(String phone, String scene, String code) {
        String normalizedPhone = PhoneValidator.normalizeAndValidate(phone);
        String normalizedScene = normalizeScene(scene);
        if (code == null || code.isBlank()) {
            throw new BusinessException("验证码不能为空");
        }
        try {
            return verifyRedis(normalizedPhone, normalizedScene, code.trim());
        } catch (RedisConnectionFailureException | IllegalStateException ex) {
            return verifyMemory(normalizedPhone, normalizedScene, code.trim());
        }
    }

    private void storeInRedis(String phone, String scene, String code) {
        String codeKey = codeKey(phone, scene);
        String attemptsKey = attemptsKey(phone, scene);
        String cooldownKey = cooldownKey(phone, scene);

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long remain = redisTemplate.getExpire(cooldownKey);
            throw new BusinessException("验证码已发送，请" + Math.max(1, remain == null ? COOLDOWN_SECONDS : remain) + "秒后再试");
        }

        redisTemplate.opsForValue().set(codeKey, code, Duration.ofSeconds(TTL_SECONDS));
        redisTemplate.opsForValue().set(attemptsKey, "0", Duration.ofSeconds(TTL_SECONDS));
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(COOLDOWN_SECONDS));
    }

    private boolean verifyRedis(String phone, String scene, String code) {
        String codeKey = codeKey(phone, scene);
        String attemptsKey = attemptsKey(phone, scene);
        String savedCode = redisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            throw new BusinessException("请先获取验证码");
        }

        String attemptsValue = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsValue == null ? 0 : Integer.parseInt(attemptsValue);
        if (attempts >= MAX_ATTEMPTS) {
            deleteRedisKeys(phone, scene);
            throw new BusinessException("验证码尝试次数过多，请重新获取");
        }

        if (!savedCode.equals(code)) {
            attempts++;
            redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts), Duration.ofSeconds(TTL_SECONDS));
            throw new BusinessException("验证码错误，还剩" + (MAX_ATTEMPTS - attempts) + "次尝试");
        }

        deleteRedisKeys(phone, scene);
        return true;
    }

    private void storeInMemory(String phone, String scene, String code) {
        String key = memoryKey(phone, scene);
        CodeEntry existing = fallbackStore.get(key);
        if (existing != null) {
            long secondsSinceCreate = Instant.now().getEpochSecond() - existing.createdAt.getEpochSecond();
            if (secondsSinceCreate < COOLDOWN_SECONDS) {
                throw new BusinessException("验证码已发送，请" + (COOLDOWN_SECONDS - secondsSinceCreate) + "秒后再试");
            }
        }
        fallbackStore.put(key, new CodeEntry(code, Instant.now(), 0));
    }

    private boolean verifyMemory(String phone, String scene, String code) {
        String key = memoryKey(phone, scene);
        CodeEntry entry = fallbackStore.get(key);
        if (entry == null) {
            throw new BusinessException("请先获取验证码");
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            fallbackStore.remove(key);
            throw new BusinessException("验证码尝试次数过多，请重新获取");
        }
        long secondsSinceCreate = Instant.now().getEpochSecond() - entry.createdAt.getEpochSecond();
        if (secondsSinceCreate > TTL_SECONDS) {
            fallbackStore.remove(key);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        entry.attempts++;
        if (!entry.code.equals(code)) {
            throw new BusinessException("验证码错误，还剩" + (MAX_ATTEMPTS - entry.attempts) + "次尝试");
        }
        fallbackStore.remove(key);
        return true;
    }

    private void deleteRedisKeys(String phone, String scene) {
        redisTemplate.delete(codeKey(phone, scene));
        redisTemplate.delete(attemptsKey(phone, scene));
        redisTemplate.delete(cooldownKey(phone, scene));
    }

    private String codeKey(String phone, String scene) {
        return "clas:sms:" + scene + ":" + phone;
    }

    private String attemptsKey(String phone, String scene) {
        return "clas:sms:" + scene + ":" + phone + ":attempts";
    }

    private String cooldownKey(String phone, String scene) {
        return "clas:sms:" + scene + ":" + phone + ":cooldown";
    }

    private String memoryKey(String phone, String scene) {
        return scene + ":" + phone;
    }

    private String normalizeScene(String scene) {
        return scene == null || scene.isBlank() ? "register" : scene.trim();
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static class CodeEntry {
        final String code;
        final Instant createdAt;
        int attempts;

        CodeEntry(String code, Instant createdAt, int attempts) {
            this.code = code;
            this.createdAt = createdAt;
            this.attempts = attempts;
        }
    }
}
