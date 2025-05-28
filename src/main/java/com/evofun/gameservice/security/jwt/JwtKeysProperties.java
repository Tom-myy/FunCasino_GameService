package com.evofun.gameservice.security.jwt;

import io.jsonwebtoken.security.Keys;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
@Setter
public class JwtKeysProperties {
    private String gameSecret;
    private String systemSecret;

    public SecretKey getGameKey() {
        return Keys.hmacShaKeyFor(gameSecret.getBytes(StandardCharsets.UTF_8));
    }

    public SecretKey getSystemKey() {
        return Keys.hmacShaKeyFor(systemSecret.getBytes(StandardCharsets.UTF_8));
    }
}
