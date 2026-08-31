package com.pixsom.racebets.auth.google;

public class OAuthAccountLinkRequiredException extends RuntimeException {
    public OAuthAccountLinkRequiredException() {
        super("Existing account confirmation required");
    }
}
