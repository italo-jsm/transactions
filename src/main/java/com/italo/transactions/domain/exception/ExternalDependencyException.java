package com.italo.transactions.domain.exception;

public class ExternalDependencyException extends RuntimeException {

    public ExternalDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
