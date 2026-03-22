package com.italo.transactions.domain.exception;

public class ExternalDependencyTimeoutException extends RuntimeException {

    public ExternalDependencyTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
