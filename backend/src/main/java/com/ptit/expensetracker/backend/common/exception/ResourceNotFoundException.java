package com.ptit.expensetracker.backend.common.exception;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(message, "NOT_FOUND");
    }
}



