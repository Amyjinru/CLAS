package com.clas.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class VerificationCodeStoreTest {
    @Test
    void expiredInMemoryCodeIsRejectedAndRemoved() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-31T00:00:00Z"));
        VerificationCodeStore store = new VerificationCodeStore(null, "123456", clock);
        store.generateAndStore("13900000050", "forgot");

        clock.advanceSeconds(601);
        BusinessException expired = assertThrows(BusinessException.class,
            () -> store.verify("13900000050", "forgot", "123456"));
        org.junit.jupiter.api.Assertions.assertEquals("验证码已过期，请重新获取", expired.getMessage());

        BusinessException removed = assertThrows(BusinessException.class,
            () -> store.verify("13900000050", "forgot", "123456"));
        org.junit.jupiter.api.Assertions.assertEquals("请先获取验证码", removed.getMessage());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
