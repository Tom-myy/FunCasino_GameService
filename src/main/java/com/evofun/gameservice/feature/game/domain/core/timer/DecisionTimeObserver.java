package com.evofun.gameservice.feature.game.domain.core.timer;

import com.evofun.gameservice.feature.game.api.websocket.message.WsMessage;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageType;
import com.evofun.gameservice.feature.game.domain.model.Player;
import com.evofun.gameservice.feature.game.utils.PlayerRegistry;
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
        for (Player p : playerRegistry.getPlayersWhoAreInGame()) {
            if (seconds == -1)
                messageSenderImpl.sendToClient(p.getUserId(), new WsMessage<>(seconds, WsMessageType.TIMER_CANCEL));
            else
                messageSenderImpl.sendToClient(p.getUserId(), new WsMessage<>(seconds, WsMessageType.TIMER));
        }
    }
}
