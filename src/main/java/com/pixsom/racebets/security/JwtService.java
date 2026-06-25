package com.pixsom.racebets.security;


import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.expiration}")
    private long jwtTokenExpiration;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(AppUser user) {
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(jwtTokenExpiration);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .claim("roles", user.getRoles().stream().map(Role::name).toList())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(jwsHeader, claims))
                .getTokenValue();
    }
}
