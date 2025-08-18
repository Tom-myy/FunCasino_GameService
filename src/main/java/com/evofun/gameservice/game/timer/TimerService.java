package com.evofun.gameservice.game.timer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TimerService {

    @Value("${game.timer.betting-time}")
    private int bettingTime;

    @Value("${game.timer.decision-time}")
    private int decisionTime;
    private final Map<TimerType, GameTimer> timers = new ConcurrentHashMap<>();



    public void start(TimerType type, TimerObserver observer) {
        stopAll();
        GameTimer timer = new GameTimer(observer);
        timers.put(type, timer);
        if (type == TimerType.BETTING_TIME)
            timer.startTimer(bettingTime);
        else if (type == TimerType.DECISION_TIME) {
            timer.startTimer(decisionTime);
        }
    }

    public void stop(TimerType type) {
        GameTimer timer = timers.remove(type);
        if (timer != null) {
            timer.stopTimer();
        }
    }

    public void suspendBettingTime(TimerType type) {
        GameTimer timer = timers.remove(type);
        if (timer != null) {
            timer.stopTimer();
        }
    }

    public void stopAll() {
        timers.values().forEach(GameTimer::stopTimer);
        timers.clear();
    }

    public boolean isRunning(TimerType type) {
        GameTimer timer = timers.get(type);
        return timer != null && timer.isRunning();
    }
}