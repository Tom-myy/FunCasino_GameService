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
public class UserInternalDto {
    private UUID userUUID;
    private String name;
    private String surname;
    private String nickname;
    private BigDecimal balance;
    //internal:
    private BigDecimal balanceDelta;

    @Override
    public String toString() {
        return "UserInternalDto{" +
                "userUUID=" + userUUID +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", nickname='" + nickname + '\'' +
                ", balance=" + balance +
                ", balanceDelta=" + balanceDelta +
                '}';
    }
}
