package com.evofun.gameservice.websocket.message;

public enum WsMessageType {
    AUTHORIZATION,//mb rename to AUTHORIZATION_PLAYER

    PLAYER_DATA,

    TABLE_STATUS,

    SEATS,
    TAKE_SEAT,
    UPDATE_SEAT_BET,
    LEAVE_SEAT,

    REQUEST_TO_START_GAME,
    GAME_STARTED,
    GAME_DECISION,
    E_GAME_RESULT_STATUS,
    DEALER,
    CURRENT_SEAT,//TODO think if it's possible to change to smth
                 // like GAME_TURN and send seatNumber instead of Seat obj
    GAME_SEAT_UPDATED,
    E_GAME_STATUS_FOR_INTERFACE,//TODO think over
    GAME_RESULT, //TODO
    GAME_FINISHED,//TODO
    USER_INFO_REFRESH,

    TIMER,
    TIMER_CANCEL,

    ERROR
}