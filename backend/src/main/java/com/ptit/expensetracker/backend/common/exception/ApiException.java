package com.ptit.expensetracker.backend.common.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final String errorCode;

    public ApiException(String message) {
        super(message);
        this.errorCode = "GENERIC_ERROR";
    }

    public ApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}



