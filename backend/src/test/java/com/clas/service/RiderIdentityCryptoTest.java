package com.clas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RiderIdentityCryptoTest {

    @Test
    void identityNumberIsEncryptedAndCanOnlyBeShownAsMaskedValueByDefault() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        RiderIdentityCrypto crypto = new RiderIdentityCrypto("clas-rider-test-key-32-bytes!!!!", environment);
        String idCardNo = "110108200001011234";

        String encrypted = crypto.encrypt(idCardNo);

        assertNotEquals(idCardNo, encrypted);
        assertEquals(idCardNo, crypto.decrypt(encrypted));
        assertEquals("110***********1234", crypto.mask(idCardNo));
    }
}
