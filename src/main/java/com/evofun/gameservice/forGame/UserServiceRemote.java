package com.evofun.gameservice.forGame;

import com.evofun.gameservice.common.error.ErrorDto;
import com.evofun.gameservice.dto.UserInternalDto;
import com.evofun.gameservice.dto.UserPublicDto;
import com.evofun.gameservice.exception.UserNotFoundException;
import com.evofun.gameservice.game.PlayerModel;
import com.evofun.gameservice.mapper.UserInternalMapper;
import com.evofun.gameservice.mapper.UserPublicMapper;
import com.evofun.gameservice.model.UserModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class UserServiceRemote {
    private final ObjectMapper objectMapper;
    private final String userServiceUrl = "http://localhost:8080/api/internal";

    public UserServiceRemote(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UserInternalDto findUserById(UUID userId) {//TODO change to WebClient!!
        String url = userServiceUrl + "/userById/" + userId;//TODO endpoint

        try {
            WebClient client = WebClient.create(userServiceUrl);

            return client.get()
//                    .uri(url)
                    .uri("/userById/" + userId)
                    .retrieve()
                    .bodyToMono(UserInternalDto.class)
                    .block();

        } catch (HttpStatusCodeException e) {
            String errorJson = e.getResponseBodyAsString();
            ErrorDto error = null;
            try {
                error = objectMapper.readValue(errorJson, ErrorDto.class);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException("Error: " + error.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Unknown error: ", e);
        }

    }
/*    public BigDecimal applyBalanceChange(UUID userId, BigDecimal delta) {
        String url = userServiceUrl + "/" + userId + "/balance";
        BalanceChangeRequest request = new BalanceChangeRequest(delta);

        ResponseEntity<BigDecimal> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                BigDecimal.class
        );

        *//*return response.getBody();*//*
        return restTemplate.patchForObject(url, request, BigDecimal.class);
    }*/

/*    public List<UserDto> updateUsersAfterGame(List<PlayerModel> models) {
        String url = userServiceUrl + "/updateUsersAfterGame";

        List<UserDto> request = models.stream()
                .map(PlayerMapper::playerModelToUserDto)
                .toList();

        HttpEntity<List<UserDto>> entity = new HttpEntity<>(request);

        ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                entity,
                new ParameterizedTypeReference<List<UserDto>>() {}
        );

        return response.getBody();
    }*/

    /*    public List<UserPublicDto> updateUsersAfterGame(List<PlayerModel> models) {//TODO change to WebClient!!
            String url = userServiceUrl + "/updateUsersAfterGame";//TODO endpoint

            List<UserInternalDto> request = models.stream()
                    .map(PlayerModel::getUserModel)
                    .map(UserInternalMapper::toInternalDto)
                    .toList();

            HttpEntity<List<UserInternalDto>> entity = new HttpEntity<>(request);

            ResponseEntity<List<UserInternalDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<UserInternalDto> body = response.getBody();
            if (body == null) {
                return Collections.emptyList();//TODO throw new RuntimeException("No response from user service");
            }

            return body.stream()
                    .map(UserPublicMapper::toPublicDto)
                    .toList();
        }*/
    public List<UserPublicDto> updateUsersAfterGame(List<PlayerModel> models) {//TODO change to WebClient!!

        List<UserInternalDto> listUserInternalDtoToUpdate = models.stream()
                .map(PlayerModel::getUserModel)         // получаем UserModel
                .map(UserInternalMapper::toInternalDto)         // конвертируем в DTO
                .toList();                              // собираем в список


        try {
            WebClient client = WebClient.create(userServiceUrl);

            List<UserInternalDto> listUserInternalDtoUpdated = client.patch()
                    .uri("/updateUsersAfterGame")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(listUserInternalDtoToUpdate)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UserInternalDto>>() {
                    })
                    .block();

            if (listUserInternalDtoUpdated == null) {
                return Collections.emptyList();//TODO throw new RuntimeException("No response from user service");
            }

            return listUserInternalDtoUpdated.stream()
                    .map(UserPublicMapper::toPublicDto)
                    .toList();

        } catch (HttpStatusCodeException e) {
            String errorJson = e.getResponseBodyAsString();
            ErrorDto error = null;
            try {
                error = objectMapper.readValue(errorJson, ErrorDto.class);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException("Error: " + error.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Unknown error: ", e);
        }
    }

/*    public UserInternalDto findUserById (UUID userId) {//TODO change to WebClient!!
        String url = userServiceUrl + "/users/by-id/" + userId;//TODO endpoint

        try {
            ResponseEntity<UserInternalDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            // JSON-ошибка от сервиса
            String errorJson = e.getResponseBodyAsString();
            ErrorDto error = null;
            try {
                error = objectMapper.readValue(errorJson, ErrorDto.class);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException("Ошибка: " + error.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Неизвестная ошибка", e);
        }

//        new UserNotFoundException("User with UUID (" + payload.userId() + ") not found in DB during authorization (WS).")
    }*/


}
