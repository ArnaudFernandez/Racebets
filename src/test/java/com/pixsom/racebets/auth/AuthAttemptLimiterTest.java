package com.pixsom.racebets.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthAttemptLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-06T12:00:00Z"));
    private final AuthAttemptLimiter limiter = new AuthAttemptLimiter(
            10, Duration.ofMinutes(15), Duration.ofMinutes(15), 10_000, clock);

    @Test
    void allowsOneAttemptForEachOf150AccountsBehindTheSameNat() {
        for (int index = 0; index < 150; index++) {
            String email = "participant-" + index + "@example.test";
            assertThatCode(() -> limiter.acquire(email)).doesNotThrowAnyException();
        }
    }

    @Test
    void blocksTheEleventhAttemptForOneAccount() {
        for (int index = 0; index < 10; index++) {
            limiter.acquire("target@example.test");
        }

        assertThatThrownBy(() -> limiter.acquire("target@example.test"))
                .isInstanceOf(AuthRateLimitExceededException.class)
                .extracting("retryAfterSeconds")
                .isEqualTo(900L);
    }

    @Test
    void successfulLoginClearsPreviousAttempts() {
        for (int index = 0; index < 10; index++) {
            limiter.acquire("target@example.test");
        }
        limiter.recordSuccess("target@example.test");

        assertThatCode(() -> limiter.acquire("target@example.test")).doesNotThrowAnyException();
    }

    @Test
    void attemptsAreAvailableAgainAfterTheWindow() {
        for (int index = 0; index < 10; index++) {
            limiter.acquire("target@example.test");
        }
        clock.advance(Duration.ofMinutes(16));

        assertThatCode(() -> limiter.acquire("target@example.test")).doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
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
