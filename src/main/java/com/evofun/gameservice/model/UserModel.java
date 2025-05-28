package com.evofun.gameservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserModel {
    private UUID userUUID;
    private String name;
    private String surname;
    private String nickname;
    private BigDecimal balance;
    @JsonIgnore
    private BigDecimal balanceDelta = BigDecimal.ZERO;

    public UserModel(UUID userUUID, String name, String surname, String nickName, BigDecimal balance) {
        this.userUUID = userUUID;
        this.name = name;
        this.surname = surname;
        this.nickname = nickName;
        this.balance = balance;
    }

    public void changeBalance(BigDecimal amount) {
        this.balance = balance.add(amount);
        balanceDelta = balanceDelta.add(amount);
    }

    @Override
    public String toString() {
        return "UserModel{" +
                "userUUID=" + userUUID +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", nickname='" + nickname + '\'' +
                ", balance=" + balance +
                ", balanceDelta=" + balanceDelta +
                '}';
    }
}
