package com.pixsom.racebets.auth;

import com.pixsom.racebets.auth.google.OAuthAccountLinkRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void handleBadCredentials() {
    }

    @ExceptionHandler(OAuthAccountLinkRequiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public AuthErrorResponse handleOAuthAccountLinkRequired() {
        return new AuthErrorResponse("ACCOUNT_LINK_REQUIRED");
    }

    @ExceptionHandler(AuthRateLimitExceededException.class)
    public ResponseEntity<AuthErrorResponse> handleRateLimit(AuthRateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(new AuthErrorResponse("AUTH_RATE_LIMITED"));
    }

    public record AuthErrorResponse(String code) {
    }
}
