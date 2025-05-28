package com.evofun.gameservice.mapper;

import com.evofun.gameservice.dto.UserInternalDto;
import com.evofun.gameservice.model.UserModel;

public class UserInternalMapper {
    public static UserInternalDto toInternalDto(UserModel userModel) {
        UserInternalDto userInternalDto = new UserInternalDto();

        userInternalDto.setUserUUID(userModel.getUserUUID());
        userInternalDto.setName(userModel.getName());
        userInternalDto.setSurname(userModel.getSurname());
        userInternalDto.setNickname(userModel.getNickname());
        userInternalDto.setBalance(userModel.getBalance());
        userInternalDto.setBalanceDelta(userModel.getBalanceDelta());

        return userInternalDto;
    }

    public static UserModel toModel(UserInternalDto userInternalDto) {//could be not full...
        UserModel userModel = new UserModel();

        userModel.setUserUUID(userInternalDto.getUserUUID());
        userModel.setName(userInternalDto.getName());
        userModel.setSurname(userInternalDto.getSurname());
        userModel.setNickname(userInternalDto.getNickname());
        userModel.setBalance(userInternalDto.getBalance());
        userModel.setBalanceDelta(userInternalDto.getBalanceDelta());

        return userModel;
    }
}
