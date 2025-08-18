package com.evofun.gameservice;

import com.evofun.gameservice.db.UserGameBalanceDto;
import com.evofun.gameservice.dto.request.MoneyReservationRequest;
import com.evofun.gameservice.exception.ServiceUnavailable;
import com.evofun.gameservice.exception.UserNotFoundException;
import com.evofun.gameservice.websocket.exception.RemoteServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MoneyServiceClient {

    @Value("${money-service.base-url}")
    private String baseMoneyServiceUrl;

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

    public UserGameBalanceDto getGameBalanceByUserId(UUID userId) {
        if (!isMoneyServiceAlive()) {
            throw new ServiceUnavailable(
                    "Money service is unavailable.",
                    "Some service is temporarily unavailable on the service."
            );
        }
        try {
            WebClient client = WebClient.create(baseMoneyServiceUrl);

            return client.get()
                    .uri("/api/internal/userBalanceById/" + userId)
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, response ->
                            Mono.error(new UserNotFoundException("User not found: " + userId, "TODO"))//TODO
                    )
                    .bodyToMono(UserGameBalanceDto.class)
                    .block();

        } catch (UserNotFoundException unfe) {
            throw unfe;
        } catch (WebClientResponseException e) {
///            log.error("User-service error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RemoteServiceException("User-service error: " + e.getStatusCode(), e);
        } catch (Exception e) {
///            log.error("Unexpected error in userServiceRemote", e);
            throw new RuntimeException("Unexpected internal error", e);
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

        try {
            client.post()
                    .uri("/api/internal/reserveMoneyForBet")
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
}
