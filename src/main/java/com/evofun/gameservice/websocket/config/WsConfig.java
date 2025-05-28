package com.evofun.gameservice.websocket.config;

import com.evofun.gameservice.websocket.handler.MainWsHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WsConfig implements WebSocketConfigurer {

    private final MainWsHandler mainWsHandler;

    public WsConfig(MainWsHandler mainWsHandler) {
        this.mainWsHandler = mainWsHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mainWsHandler, "/ws").setAllowedOrigins("*");
    }
}
