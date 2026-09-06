package com.pixsom.racebets.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "JWT_SECRET=test-secret-with-enough-length-for-hs256-signature",
        "jwt.expiration=3600000"
})
@AutoConfigureMockMvc
@Import({SecurityConfigTest.TestAdminController.class, SecurityConfigTest.TestRealtimeController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void jwtAuthenticationConverterMapsRolesClaimToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("signed.jwt.token")
                .header("alg", "HS256")
                .subject("admin@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("roles", List.of("ADMIN", "VIP"))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN", "ROLE_VIP");
    }

    @Test
    void adminEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonPublicEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/realtime/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicWordCloudEndpointAcceptsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/word-cloud/public/live"))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminEndpointRejectsAuthenticatedUserWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/health")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenWithRoles(List.of("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointAcceptsAuthenticatedAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/health")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenWithRoles(List.of("ADMIN"))))
                .andExpect(status().isOk());
    }

    private String bearerTokenWithRoles(List<String> roles) {
        Instant now = Instant.now();
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("admin@example.com")
                .claim("roles", roles)
                .claim("userId", 1L)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
        return "Bearer " + token;
    }

    @Controller
    @RequestMapping("/api/admin")
    static class TestAdminController {

        @GetMapping("/health")
        ResponseEntity<Void> health() {
            return ResponseEntity.ok().build();
        }
    }

    @Controller
    @RequestMapping("/api/realtime")
    static class TestRealtimeController {

        @GetMapping("/health")
        ResponseEntity<Void> health() {
            return ResponseEntity.ok().build();
        }
    }
}
