package com.evofun.gameservice.game;

import com.evofun.gameservice.model.SeatModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class PlayerModel {
    @Getter
    @Setter
    private UUID userId;
    @Getter
    @Setter
    private String nickname;
    @Getter
    @Setter
    private BigDecimal balance;
    //TODO mb delete
    @JsonIgnore
    @Getter
    @Setter
    private BigDecimal balanceDelta = BigDecimal.ZERO;

    @Getter
    private List<SeatModel> seatModels = new ArrayList<>();
    @Getter
    @Setter
    private boolean inTheGame = false;


    //model fields:

    @Getter
    @Setter
    private boolean wantsToStartGame = false;

    public void resetBalanceDifference() {
        setBalanceDelta(BigDecimal.ZERO);
    }

    public void addSeat(SeatModel seatModel) {
        seatModels.add(seatModel);
    }

    public void changeBalance(BigDecimal amount) {
        this.balance = balance.add(amount);
        balanceDelta = balanceDelta.add(amount);
    }

    public BigDecimal getTotalBet() {
        if (seatModels.isEmpty() || seatModels == null) return BigDecimal.ZERO;

        BigDecimal totalBet = BigDecimal.ZERO;
        for (SeatModel seatModel : seatModels) {
            totalBet = totalBet.add(seatModel.getCurrentBet());
        }
        return totalBet;
    }

    public void restartBeforeGame() {
        wantsToStartGame = false;

        seatModels.stream()
                .filter(SeatModel::isInTheGame)
                .forEach(SeatModel::restartBeforeGame);
    }

    public void restartAfterGame() {
        inTheGame = false;
        wantsToStartGame = false;
        setBalanceDelta(BigDecimal.ZERO);

        seatModels.stream()
                .filter(SeatModel::isInTheGame)
                .forEach(SeatModel::restartAfterGame);
    }
}
