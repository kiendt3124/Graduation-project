package com.ptit.expensetracker.backend.common.exception;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }
}



