package com.evofun.gameservice.security.jwt;

import com.evofun.gameservice.model.UserModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private final Key SECRET_AUTH_KEY = Keys.hmacShaKeyFor("evofun-production-secret-auth-key-2025-abc123$$".getBytes(StandardCharsets.UTF_8));
    private final Key SECRET_GAME_KEY = Keys.hmacShaKeyFor("evofun-production-secret-game-key-2025-abc123$$".getBytes(StandardCharsets.UTF_8));
    private final Key SECRET_SYSTEM_KEY = Keys.hmacShaKeyFor("evofun-production-secret-system-key-2025-abc123$$".getBytes(StandardCharsets.UTF_8));

/*    public JwtResponse generateResponse(User user) {
        return new JwtResponse(generateToken(user), user.getUserID(), user.getNickname());
    }*//// mb for game JWT token (not web)

    public JwtPayload extractPayload(String token) {
        Claims claims = extractAllClaims(token);
        return new JwtPayload(
                UUID.fromString(claims.getSubject()),
                claims.get("nickName", String.class)
        );
    }

    private String generateToken(UserModel userModel) {
        return Jwts.builder()
                .setSubject(userModel.getUserUUID().toString())
                .claim("nickName", userModel.getNickname())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(SECRET_AUTH_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_AUTH_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

