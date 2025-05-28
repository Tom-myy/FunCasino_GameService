package com.evofun.gameservice.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FieldErrorDto {//TODO internal or public?
    private String field;
    private String message;
}