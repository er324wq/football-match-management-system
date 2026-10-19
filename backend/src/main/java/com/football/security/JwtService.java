package com.football.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expirationMillis;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void initialize() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        if (expirationMillis <= 0) {
            throw new IllegalStateException("jwt.expiration must be positive");
        }
        algorithm = Algorithm.HMAC256(secret);
        verifier = JWT.require(algorithm).build();
    }

    public String createToken(Long userId, String username, String role) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMillis);
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withClaim("role", role)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        DecodedJWT decoded = verifier.verify(token);
        if (decoded.getClaim("userId").isNull()
                || decoded.getClaim("username").isNull()
                || decoded.getClaim("role").isNull()) {
            throw new IllegalArgumentException("Required JWT claims are missing");
        }
        return decoded;
    }
}
