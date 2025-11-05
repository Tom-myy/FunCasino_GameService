package com.evofun.gameservice.shared.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    @Value("${jwt.secrets.user}")
    String userSecret;
    @Value("${jwt.secrets.internal}")
    String internalSecret;

    @Value("${security.user.issuer}")
    private String userIssuer;
    @Value("${security.internal.issuer}")
    private String internalIssuer;

    public JwtUtil() {
    }

    public JwtUser extractPayloadFromToken(String token) {
        Claims claims = extractAllClaimsFromToken(token);
        return new JwtUser(
                UUID.fromString(claims.getSubject()),
                claims.get("nickname", String.class)
        );
    }

    private Claims extractAllClaimsFromToken(String token) {
        SecretKey key = new SecretKeySpec(userSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateInternalToken() {
        SecretKey key = new SecretKeySpec(internalSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        return Jwts.builder()
                .setIssuer(internalIssuer)
                .setSubject("game-service")
                .setAudience("money-service")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .signWith(key)
                .compact();
    }
}

