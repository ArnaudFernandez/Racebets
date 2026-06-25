package com.pixsom.racebets.security;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-enough-length-for-hs256-signature";

    @Test
    void generateTokenSignsExpectedClaimsWithHs256() {
        SecretKey secretKey = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtService jwtService = new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(secretKey)));
        ReflectionTestUtils.setField(jwtService, "jwtTokenExpiration", 3_600_000L);

        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 42L);
        user.setEmail("bettor@example.com");
        user.setRoles(Set.of(Role.USER, Role.VIP));

        String token = jwtService.generateToken(user);

        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("bettor@example.com");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactlyInAnyOrder("USER", "VIP");
        assertThat(jwt.getClaimAsString("userId")).isEqualTo("42");
        assertThat(jwt.getHeaders().get("alg").toString()).isEqualTo("HS256");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toMillis()).isEqualTo(3_600_000L);
    }
}
