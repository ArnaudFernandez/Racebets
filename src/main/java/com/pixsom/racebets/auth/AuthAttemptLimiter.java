package com.pixsom.racebets.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class AuthAttemptLimiter {

    private final Cache<String, AttemptState> attempts;
    private final int maxAttempts;
    private final Duration window;
    private final Duration blockDuration;
    private final Clock clock;

    @Autowired
    public AuthAttemptLimiter(
            @Value("${racebets.auth.max-attempts:10}") int maxAttempts,
            @Value("${racebets.auth.attempt-window:PT15M}") Duration window,
            @Value("${racebets.auth.block-duration:PT15M}") Duration blockDuration,
            @Value("${racebets.auth.max-tracked-accounts:100000}") long maxTrackedAccounts
    ) {
        this(maxAttempts, window, blockDuration, maxTrackedAccounts, Clock.systemUTC());
    }

    AuthAttemptLimiter(int maxAttempts, Duration window, Duration blockDuration,
                       long maxTrackedAccounts, Clock clock) {
        if (maxAttempts < 1 || window.isNegative() || window.isZero()
                || blockDuration.isNegative() || blockDuration.isZero() || maxTrackedAccounts < 1) {
            throw new IllegalArgumentException("Authentication rate-limit settings must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.blockDuration = blockDuration;
        this.clock = clock;
        this.attempts = Caffeine.newBuilder()
                .maximumSize(maxTrackedAccounts)
                .expireAfterAccess(window.plus(blockDuration))
                .build();
    }

    public void acquire(String normalizedEmail) {
        AttemptState state = attempts.get(accountKey(normalizedEmail), ignored -> new AttemptState());
        Instant now = clock.instant();
        synchronized (state) {
            if (state.blockedUntil != null && now.isBefore(state.blockedUntil)) {
                throw blocked(state.blockedUntil, now);
            }
            if (state.windowStartedAt == null || !now.isBefore(state.windowStartedAt.plus(window))) {
                state.windowStartedAt = now;
                state.attempts = 0;
                state.blockedUntil = null;
            }
            if (state.attempts >= maxAttempts) {
                state.blockedUntil = now.plus(blockDuration);
                throw blocked(state.blockedUntil, now);
            }
            state.attempts++;
        }
    }

    public void recordSuccess(String normalizedEmail) {
        attempts.invalidate(accountKey(normalizedEmail));
    }

    private AuthRateLimitExceededException blocked(Instant blockedUntil, Instant now) {
        long seconds = Math.max(1, Duration.between(now, blockedUntil).toSeconds());
        return new AuthRateLimitExceededException(seconds);
    }

    private String accountKey(String email) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(email.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static final class AttemptState {
        private Instant windowStartedAt;
        private Instant blockedUntil;
        private int attempts;
    }
}
