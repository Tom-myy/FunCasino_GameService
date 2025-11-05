package com.evofun.gameservice.feature.game.domain.model;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Player {
    @Getter
    @Setter
    private UUID userId;
    @Getter
    @Setter
    private String nickname;
    @Getter
    @Setter
    private BigDecimal gameProfit = BigDecimal.ZERO;
    @Getter
    private List<Seat> seats = new ArrayList<>();
    @Getter
    @Setter
    private boolean inTheGame = false;
    @Getter
    @Setter
    private boolean wantsToStartGame = false;

    public void addSeat(Seat seat) {
        seats.add(seat);
    }

    public void changeGameProfit(BigDecimal amount) {
        this.gameProfit = gameProfit.add(amount);
    }

    public BigDecimal getTotalBet() {
        if (seats.isEmpty() || seats == null) return BigDecimal.ZERO;

        BigDecimal totalBet = BigDecimal.ZERO;
        for (Seat seat : seats) {
            totalBet = totalBet.add(seat.getCurrentBet());
        }
        return totalBet;
    }

    public void restartBeforeGame() {
        wantsToStartGame = false;

        seats.stream()
                .filter(Seat::isInTheGame)
                .forEach(Seat::restartBeforeGame);
    }

    public void restartAfterGame() {
        inTheGame = false;
        wantsToStartGame = false;
        setGameProfit(BigDecimal.ZERO);

        seats.stream()
                .filter(Seat::isInTheGame)
                .forEach(Seat::restartAfterGame);
    }
}
