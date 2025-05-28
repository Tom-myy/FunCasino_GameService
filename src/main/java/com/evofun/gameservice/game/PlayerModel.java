package com.evofun.gameservice.game;

import com.evofun.gameservice.model.UserModel;
import com.evofun.gameservice.model.SeatModel;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class PlayerModel {
    @Getter
    @Setter
    private UserModel userModel;
    @Getter
    private List<SeatModel> seatModels = new ArrayList<>();
    @Getter
    @Setter
    private boolean inTheGame = false;

    //model fields:
    @Getter
    @Setter
    private boolean wantsToStartGame = false;

    public UUID getPlayerUUID(){//TODO mb delete
        return userModel.getUserUUID();
    }

    public void resetBalanceDifference(){
        userModel.setBalanceDelta(BigDecimal.ZERO);
    }

    public void addSeat(SeatModel seatModel) {
        seatModels.add(seatModel);
    }

    public BigDecimal getTotalBet() {
        if(seatModels.isEmpty() || seatModels == null) return BigDecimal.ZERO;

        BigDecimal totalBet = BigDecimal.ZERO;
        for (SeatModel seatModel : seatModels) {
            totalBet = totalBet.add(seatModel.getCurrentBet());
        }
        return totalBet;
    }
}
