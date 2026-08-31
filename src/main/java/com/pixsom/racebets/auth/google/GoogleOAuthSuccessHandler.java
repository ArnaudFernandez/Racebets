package com.pixsom.racebets.auth.google;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "racebets.google.enabled", havingValue = "true")
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthSuccessHandler.class);

    private final GoogleAuthService googleAuthService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public GoogleOAuthSuccessHandler(GoogleAuthService googleAuthService,
                                     OAuth2AuthorizedClientService authorizedClientService) {
        this.googleAuthService = googleAuthService;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
                throw new IllegalStateException("Google did not return an OpenID Connect identity");
            }

            authorizedClientService.removeAuthorizedClient("google", authentication.getName());
            String code = googleAuthService.issueLoginCode(oidcUser);
            clearTemporarySession(request);
            response.sendRedirect("/login#google=success&code=" + code);
        } catch (RuntimeException exception) {
            LOGGER.warn("Google authentication callback failed", exception);
            clearTemporarySession(request);
            response.sendRedirect("/login#google=error");
        }
    }

    private void clearTemporarySession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
