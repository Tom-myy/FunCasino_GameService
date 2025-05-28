package com.evofun.gameservice.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDto {//TODO internal or public?
    private ErrorCode errorCode;
    private String errorId;
    private String message;
    private List<FieldErrorDto> errors;
}