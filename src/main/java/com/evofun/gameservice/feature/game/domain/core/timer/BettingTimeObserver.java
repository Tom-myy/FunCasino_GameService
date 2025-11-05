package com.evofun.gameservice.feature.game.domain.core.timer;

import com.evofun.gameservice.feature.game.api.websocket.message.WsMessage;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageSenderImpl;
import com.evofun.gameservice.feature.game.api.websocket.message.WsMessageType;
import lombok.Setter;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class BettingTimeObserver implements TimerObserver {
    private static final ExecutorService messageExecutor = Executors.newCachedThreadPool();
    @Setter
    private Runnable onTimeout;

    private final WsMessageSenderImpl messageSenderImpl;

    public BettingTimeObserver(WsMessageSenderImpl messageSenderImpl) {
        this.messageSenderImpl = messageSenderImpl;
    }

    @Override
    public void timeWasChanged(int seconds) {
        if (seconds == -1 && onTimeout != null) {
            messageExecutor.submit(() -> {
                messageSenderImpl.broadcast(new WsMessage<>("", WsMessageType.TIMER_CANCEL));//TODO mustn't broadcast - playersInGameSession at the table\in the game
            });

            onTimeout.run();
        } else
            messageExecutor.submit(() -> {
                messageSenderImpl.broadcast(new WsMessage<>(seconds, WsMessageType.TIMER));//TODO mustn't broadcast - playersInGameSession at the table\in the game
            });

    }
}
