package com.clas.common;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 忘记密码验证码内存存储。
 * 参照 auth-flow 技能的 verification-store.ts：
 * - 6位数字验证码
 * - 10分钟过期
 * - 30秒冷却
 * - 5次验证尝试上限
 * - 验证通过后立即销毁
 * - 服务器重启全部失效（无持久化）
 */
public class VerificationCodeStore {

    private static final Map<String, CodeEntry> STORE = new ConcurrentHashMap<>();

    /** 验证码有效期：10分钟 */
    private static final long TTL_SECONDS = 10 * 60;

    /** 发送冷却：30秒 */
    private static final long COOLDOWN_SECONDS = 30;

    /** 最大验证尝试次数 */
    private static final int MAX_ATTEMPTS = 5;

    /**
     * 生成6位数字验证码并存储。
     * @return 生成的验证码（仅用于后端日志输出，不返回给前端）
     * @throws BusinessException 冷却期内重复请求
     */
    public static String generateAndStore(String phone) {
        CodeEntry existing = STORE.get(phone);
        if (existing != null) {
            long secondsSinceCreate = Instant.now().getEpochSecond() - existing.createdAt.getEpochSecond();
            if (secondsSinceCreate < COOLDOWN_SECONDS) {
                long remain = COOLDOWN_SECONDS - secondsSinceCreate;
                throw new BusinessException("验证码已发送，请" + remain + "秒后再试");
            }
        }
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        STORE.put(phone, new CodeEntry(code, Instant.now(), 0));
        // 模拟短信发送 — 实际项目中此处接入短信SDK
        System.out.println("[VerificationCode] 手机号 " + maskPhone(phone) + " 的验证码: " + code);
        return code;
    }

    /**
     * 验证码校验。
     * @return true 表示验证通过
     * @throws BusinessException 验证码不存在、已过期、或尝试次数超限
     */
    public static boolean verify(String phone, String code) {
        CodeEntry entry = STORE.get(phone);
        if (entry == null) {
            throw new BusinessException("请先获取验证码");
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            STORE.remove(phone);
            throw new BusinessException("验证码尝试次数过多，请重新获取");
        }
        long secondsSinceCreate = Instant.now().getEpochSecond() - entry.createdAt.getEpochSecond();
        if (secondsSinceCreate > TTL_SECONDS) {
            STORE.remove(phone);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        entry.attempts++;
        if (!entry.code.equals(code)) {
            int remaining = MAX_ATTEMPTS - entry.attempts;
            throw new BusinessException("验证码错误，还剩" + remaining + "次尝试");
        }
        // 验证通过，立即销毁
        STORE.remove(phone);
        return true;
    }

    /**
     * 检查手机号是否已发送过验证码（冷却检查）。
     */
    public static long getCooldownSeconds(String phone) {
        CodeEntry entry = STORE.get(phone);
        if (entry == null) return 0;
        long secondsSinceCreate = Instant.now().getEpochSecond() - entry.createdAt.getEpochSecond();
        return Math.max(0, COOLDOWN_SECONDS - secondsSinceCreate);
    }

    /** 手机号脱敏：138****0001 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
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
