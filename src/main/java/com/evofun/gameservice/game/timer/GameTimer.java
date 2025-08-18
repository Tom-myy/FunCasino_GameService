package com.evofun.gameservice.game.timer;

import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameTimer {
    private static final ExecutorService timerExecutor = Executors.newCachedThreadPool();

    private TimerObserver observer;
    @Getter
    private volatile boolean isRunning = false;
    @Getter
    private volatile int time = -1;


    public GameTimer(TimerObserver observer) {
        this.observer = observer;
    }

    public synchronized void startTimer(int seconds) {
        if (isRunning) return;

        isRunning = true;
        time = seconds;

        timerExecutor.submit(() -> {
            notifyObserver(time);

            while (time > 0 && isRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                time--;
                if (isRunning) notifyObserver(time);
            }

            stopTimer();
        });
    }

    public synchronized void stopTimer() {
        isRunning = false;
        time = -1;

        notifyObserver(-1);
    }

    @Getter
    private volatile int suspendedTime;

    public synchronized void suspendTimer() {
        isRunning = false;
        suspendedTime = time;
    }

    public synchronized void continueTimer() {
        if (isRunning) return;

        isRunning = true;
        time = --suspendedTime;

        timerExecutor.submit(() -> {
            notifyObserver(time);

            while (time > 0 && isRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                time--;
                if (isRunning) notifyObserver(time);
            }

            stopTimer();
        });
    }

    private void notifyObserver(int time) {
        observer.timeWasChanged(time);
    }
}
