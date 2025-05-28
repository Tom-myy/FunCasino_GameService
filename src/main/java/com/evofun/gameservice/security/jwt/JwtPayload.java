package com.evofun.gameservice.security.jwt;

import java.util.UUID;

public record JwtPayload(
        UUID userId,
        String nickName
) {}
