package com.clas.catalog.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternalApiKeyVerifier {
    private final byte[] expectedKey;

    public InternalApiKeyVerifier(@Value("${catalog.internal-api-key}") String internalApiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException("CATALOG_INTERNAL_API_KEY must be configured");
        }
        this.expectedKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String suppliedKey) {
        byte[] candidate = suppliedKey == null ? new byte[0] : suppliedKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedKey, candidate)) {
            throw new InternalAuthenticationException();
        }
    }
}
