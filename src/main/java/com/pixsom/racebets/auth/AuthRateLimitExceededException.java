package com.pixsom.racebets.auth;

public class AuthRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public AuthRateLimitExceededException(long retryAfterSeconds) {
        super("Too many authentication attempts");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
