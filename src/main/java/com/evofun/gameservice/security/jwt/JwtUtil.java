package com.evofun.gameservice.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class JwtUtil {
    private final JwtKeysProperties jwtKeysProperties;

    public JwtUtil(JwtKeysProperties jwtKeysProperties) {
        this.jwtKeysProperties = jwtKeysProperties;
    }

    public JwtUser extractPayloadFromGameToken(String token) {
        Claims claims = extractAllClaimsFromGameToken(token);
        return new JwtUser(
                UUID.fromString(claims.getSubject()),
                claims.get("nickName", String.class)
        );
    }

    private Claims extractAllClaimsFromGameToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtKeysProperties.getGameKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

/*    private String generateToken(UserModel userModel) {
        return Jwts.builder()
                .setSubject(userModel.getUserUUID().toString())
                .claim("nickName", userModel.getNickname())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(SECRET_AUTH_KEY, SignatureAlgorithm.HS256)
                .compact();
    }*/
}

