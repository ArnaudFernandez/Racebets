package com.pixsom.racebets.auth;

import com.pixsom.racebets.auth.google.OAuthAccountLinkRequiredException;
import org.springframework.http.HttpStatus;
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

    public record AuthErrorResponse(String code) {
    }
}
