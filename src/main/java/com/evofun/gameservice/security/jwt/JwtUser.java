package com.evofun.gameservice.security.jwt;

import java.security.Principal;
import java.util.UUID;

public class JwtUser implements Principal {
    private final UUID id;

    public JwtUser(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return id.toString();
    }
}
