package com.evofun.gameservice.game.timer;

import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.game.PlayerRegistry;
import com.evofun.gameservice.websocket.message.WsMessage;
import com.evofun.gameservice.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.websocket.message.WsMessageType;
import org.springframework.stereotype.Component;

@Component
public class DecisionTimeObserver implements TimerObserver {
    private final WsMessageSenderImpl messageSenderImpl;
    private final PlayerRegistry playerRegistry;

    public DecisionTimeObserver(WsMessageSenderImpl messageSenderImpl, PlayerRegistry playerRegistry) {
        this.messageSenderImpl = messageSenderImpl;
        this.playerRegistry = playerRegistry;
    }

    @Override
    public void timeWasChanged(int seconds) {
        for (PlayerModel p : playerRegistry.getPlayersWhoAreInGame()) {
            if (seconds == -1)
                messageSenderImpl.sendToClient(p.getUserId(), new WsMessage<>(seconds, WsMessageType.TIMER_CANCEL));
            else
                messageSenderImpl.sendToClient(p.getUserId(), new WsMessage<>(seconds, WsMessageType.TIMER));
        }
    }
}
