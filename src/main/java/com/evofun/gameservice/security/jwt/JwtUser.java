package com.evofun.gameservice.security.jwt;

import lombok.Getter;
import lombok.Setter;

import java.security.Principal;
import java.util.UUID;

@Getter
@Setter
public class JwtUser implements Principal {
    private final UUID userId;
    private final String nickname;

    public JwtUser(UUID userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}