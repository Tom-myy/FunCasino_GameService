package com.evofun.gameservice.websocket.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WsMessage<T>  {

    private WsMessageType wsMessageType;
    private T message;

    public WsMessage() {}
    public WsMessage(T message, WsMessageType wsMessageType) {
        this.message = message;
        this.wsMessageType = wsMessageType;
    }
}
