package com.evofun.gameservice.kafka;

import com.evofun.events.GameFinishedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {
    private final KafkaTemplate<String, GameFinishedEvent> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, GameFinishedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendGameFinishedEvent(GameFinishedEvent event) {
        kafkaTemplate.send("game-finished", event.userId().toString(), event);
    }

}