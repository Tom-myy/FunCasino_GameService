package com.evofun.gameservice.feature.game.infrastructure.integration;

import com.evofun.gameservice.feature.game.api.websocket.exception.ServiceUnavailable;
import com.evofun.gameservice.feature.game.infrastructure.integration.dto.BetCancelRequest;
import com.evofun.gameservice.feature.game.infrastructure.integration.dto.MoneyReservationRequest;
import com.evofun.gameservice.shared.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MoneyServiceClient {
    @Value("${clients.services.money.base-url}")
    private String baseMoneyServiceUrl;

    private final JwtUtil jwtUtil;

    public MoneyServiceClient(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    private boolean isMoneyServiceAlive() {
        try {
            WebClient client = WebClient.create(baseMoneyServiceUrl);

            client.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean reserveMoneyForBet(UUID userId, BigDecimal bet) {
        if (!isMoneyServiceAlive()) {
            throw new ServiceUnavailable(
                    "Money service is unavailable.",
                    "Some service is temporarily unavailable on the service."
            );
        }

        MoneyReservationRequest moneyReservationRequest = new MoneyReservationRequest(userId, bet);

        WebClient client = WebClient.create(baseMoneyServiceUrl);

        String internalToken = jwtUtil.generateInternalToken();
        try {
            client.post()
                    .uri("/api/v1/money/reservation/reserveMoneyForBet")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(internalToken))
                    .bodyValue(moneyReservationRequest)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return true;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                return false;
            }
            throw e;
        }
    }

    public void cancelBet(UUID userId, BigDecimal bet) {
        if (!isMoneyServiceAlive()) {
            throw new ServiceUnavailable(
                    "Money service is unavailable.",
                    "Some service is temporarily unavailable on the service."
            );
        }

        BetCancelRequest betCancelRequest = new BetCancelRequest(userId, bet);

        WebClient client = WebClient.create(baseMoneyServiceUrl);

        String internalToken = jwtUtil.generateInternalToken();
        try {
            client.post()
                    .uri("/api/v1/money/reservation/cancelBet")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(internalToken))
                    .bodyValue(betCancelRequest)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
