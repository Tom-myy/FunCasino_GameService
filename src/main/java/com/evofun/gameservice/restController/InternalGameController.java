package com.evofun.gameservice.restController;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/api/game-service")
public class InternalGameController {
    @Value("${game.ws-base-url}")
    private String wsBaseUrl;
/*
    @GetMapping("/game-service/health")
    public ResponseEntity<String> checkHealth() {

        System.out.println("Got request to check health from user-service");

        new Thread(()->{
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            WebClient client = WebClient.create("http://localhost:8080");

            String str = client.get()//TODO understand...
//                    .uri("/actuator/health")
                    .uri("/api/internal/updateUsersAfterGame")
                    .retrieve()
//                    .toBodilessEntity()   // ничего не парсим
                    .bodyToMono(String.class)
                    .block();             // ждём завершения запроса

            System.out.println(str);

        }).start();

        return ResponseEntity.ok("Ok from gameService");
    }*/


    @GetMapping("/gameAccess")
    public ResponseEntity<String> getAccessToGame() {

        System.out.println("Got request to get WS link from user-service");

/*        new Thread(()->{
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            WebClient client = WebClient.create("http://localhost:8080");

            String str = client.get()//TODO understand...
//                    .uri("/actuator/health")
                    .uri("/api/internal/updateUsersAfterGame")
                    .retrieve()
//                    .toBodilessEntity()   // ничего не парсим
                    .bodyToMono(String.class)
                    .block();             // ждём завершения запроса

            System.out.println(str);

        }).start();*/

        return ResponseEntity.ok(wsBaseUrl);
    }
}
