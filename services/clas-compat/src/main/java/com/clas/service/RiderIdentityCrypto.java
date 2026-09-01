package com.clas.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RiderIdentityCrypto {
    private final byte[] key;
    public RiderIdentityCrypto(@Value("${rider.identity-encryption-key:}") String configured, Environment environment) {
        String value = configured == null ? "" : configured.trim();
        boolean test = java.util.Arrays.asList(environment.getActiveProfiles()).contains("test");
        if (value.isBlank() && !test) throw new IllegalStateException("RIDER_IDENTITY_ENCRYPTION_KEY must be configured");
        if (value.isBlank()) value = "clas-rider-test-key-32-bytes!!!!";
        this.key = java.util.Arrays.copyOf(value.getBytes(StandardCharsets.UTF_8), 32);
    }
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] all = new byte[iv.length + cipherText.length]; System.arraycopy(iv, 0, all, 0, iv.length); System.arraycopy(cipherText, 0, all, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(all);
        } catch (Exception ex) { throw new IllegalStateException("无法加密骑手身份证信息", ex); }
    }
    public String decrypt(String encrypted) {
        try {
            byte[] all = Base64.getDecoder().decode(encrypted);
            byte[] iv = java.util.Arrays.copyOfRange(all, 0, 12);
            byte[] cipherText = java.util.Arrays.copyOfRange(all, 12, all.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) { throw new IllegalStateException("无法解密骑手身份证信息", ex); }
    }
    public String mask(String idCardNo) { return idCardNo.substring(0, 3) + "***********" + idCardNo.substring(idCardNo.length() - 4); }
}
