/*
package com.evofun.gameservice.mapper;

//import com.evofun.gameservice.model.UserModel;

public class UserPublicMapper {
*/
/*    public static UserPublicDto toPublicDto(UserModel userModel) {
        UserPublicDto userPublicDto = new UserPublicDto();

        userPublicDto.setUserId(userModel.getUserId());
        userPublicDto.setName(userModel.getName());
        userPublicDto.setSurname(userModel.getSurname());
        userPublicDto.setNickname(userModel.getNickname());
        userPublicDto.setBalance(userModel.getBalance());

        return userPublicDto;
    }*//*


    public static UserPublicDto toPublicDto(UserInternalDto userInternalDto) {
        UserPublicDto userPublicDto = new UserPublicDto();

        userPublicDto.setUserId(userInternalDto.getUserUUID());
        userPublicDto.setName(userInternalDto.getName());
        userPublicDto.setSurname(userInternalDto.getSurname());
        userPublicDto.setNickname(userInternalDto.getNickname());
        userPublicDto.setBalance(userInternalDto.getBalance());

        return userPublicDto;
    }

*/
/*    public static UserModel toModel(UserPublicDto userDto) {TODO fuck.. and mb unneeded!
        UserModel userModel = new UserModel();

        userModel.setUserId(userDto.getUserId());
        userModel.setName(userDto.getName());
        userModel.setSurname(userDto.getSurname());
        userModel.setNickname(userDto.getNickname());
        userModel.setBalance(userDto.getBalance());

        return userModel;
    }*//*

}
*/
