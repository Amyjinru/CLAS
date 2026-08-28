package com.clas.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtUtilTest {
    private static final String DEV_SECRET = "clas-dev-secret-key-2026-must-be-32bytes!";
    private static final String TEST_SECRET = "clas-test-secret-key-2026-must-be-32bytes!";

    @Test
    void rejectsDefaultSecretOutsideTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> new JwtUtil(DEV_SECRET, 86400000, environment));
    }

    @Test
    void acceptsTestSecretInTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        assertDoesNotThrow(() -> new JwtUtil(TEST_SECRET, 86400000, environment));
    }
}
