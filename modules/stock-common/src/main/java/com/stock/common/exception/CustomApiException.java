package com.stock.common.exception;

import org.springframework.http.HttpStatus;

import java.io.Serializable;

public class CustomApiException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public CustomApiException(HttpStatus status, String message){
        super(message);
        this.status = status;
    }

    public CustomApiException(HttpStatus status, Throwable e, String errorMessage) {
        super(errorMessage, e);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
