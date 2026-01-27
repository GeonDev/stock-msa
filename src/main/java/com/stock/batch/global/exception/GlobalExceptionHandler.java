package com.stock.batch.global.exception;


import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private final String errorForm = "%s (%s)";

    @ExceptionHandler(CustomApiException.class)
    public ResponseEntity<Map<String, Object>> handleCustomApiException(CustomApiException ex) {

        Map<String, Object> body = Map.of(
                "errorCode", ex.getStatus().value(),
                "errorMessage",  String.format(errorForm, ex.getStatus().getReasonPhrase(),  ex.getMessage() )
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleArgumentException(Exception ex) {
        Map<String, Object> body = Map.of(
                "errorCode", HttpStatus.BAD_REQUEST.value(),
                "errorMessage", String.format(errorForm, HttpStatus.BAD_REQUEST.getReasonPhrase(),  ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        Map<String, Object> body = Map.of(
                "errorCode", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "errorMessage", String.format(errorForm, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

}
