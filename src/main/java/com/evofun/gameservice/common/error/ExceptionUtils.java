package com.evofun.gameservice.common.error;

import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ExceptionUtils {
/*    public static String extractValidationDetails(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString();
                    String message = violation.getMessage();
                    return field + ": " + message;
                })
                .collect(Collectors.joining("; "));
    }*/
public static List<FieldErrorDto> extractFieldErrors(ConstraintViolationException ex) {
    return ex.getConstraintViolations().stream()
            .map(v -> new FieldErrorDto(
                    v.getPropertyPath().toString(),
                    v.getMessage()
            ))
            .collect(Collectors.toList());
}


    public static String generateErrorId(String prefix) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String shortUuid = UUID.randomUUID().toString().substring(0, 6);
        return prefix + "-" + timestamp + "-" + shortUuid;
    }

    private ExceptionUtils() {}
}
