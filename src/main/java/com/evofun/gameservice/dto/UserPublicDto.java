package com.evofun.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserPublicDto {
    private UUID playerUUID;
    private String name;
    private String surname;
    private String nickname;
    private BigDecimal balance;
}
