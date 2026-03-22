package com.italo.transactions.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.italo.transactions.domain.exception.EntityNotFoundException;
import com.italo.transactions.domain.exception.ExchangeRateNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.context.MessageSource;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldValidationError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(this::toFieldValidationError)
                .toList();

        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse("Request validation failed", fieldErrors));
    }

    @ExceptionHandler(ExchangeRateNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleExchangeRateNotFound(ExchangeRateNotFoundException exception) {
        return ResponseEntity.unprocessableContent()
                .body(buildBusinessErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiErrorResponse.FieldValidationError> fieldErrors = exception.getConstraintViolations()
                .stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(this::toFieldValidationError)
                .toList();

        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse("Request validation failed", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        ApiErrorResponse.FieldValidationError fieldError = extractFieldError(exception);
        if (fieldError != null) {
            return ResponseEntity.badRequest()
                    .body(buildValidationErrorResponse("Request validation failed", List.of(fieldError)));
        }

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Malformed request body",
                        List.of()
                ));
    }

    private ApiErrorResponse.FieldValidationError extractFieldError(HttpMessageNotReadableException exception) {
        JsonMappingException jsonMappingException = findCause(exception, JsonMappingException.class);
        if (jsonMappingException != null) {
            String field = extractFieldName(jsonMappingException.getPath());
            if (field != null) {
                String messageKey = switch (field) {
                    case "transactionDate" -> "validation.purchase.transactionDate.invalid";
                    default -> null;
                };

                if (messageKey != null) {
                    return new ApiErrorResponse.FieldValidationError(
                            field,
                            messageSource.getMessage(messageKey, null, Locale.getDefault())
                    );
                }
            }
        }

        String exceptionMessage = exception.getMessage();
        if (exceptionMessage != null && exceptionMessage.contains("java.time.LocalDate")) {
            return new ApiErrorResponse.FieldValidationError(
                    "transactionDate",
                    messageSource.getMessage("validation.purchase.transactionDate.invalid", null, Locale.getDefault())
            );
        }
        return null;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private ApiErrorResponse buildBusinessErrorResponse(
            String message
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                message,
                message,
                Collections.emptyList()
        );
    }

    private ApiErrorResponse buildValidationErrorResponse(
            String message,
            List<ApiErrorResponse.FieldValidationError> fieldErrors
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                fieldErrors
        );
    }

    private ApiErrorResponse.FieldValidationError toFieldValidationError(FieldError fieldError) {
        return new ApiErrorResponse.FieldValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

    private ApiErrorResponse.FieldValidationError toFieldValidationError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.contains(".")
                ? path.substring(path.lastIndexOf('.') + 1)
                : path;

        return new ApiErrorResponse.FieldValidationError(
                field,
                violation.getMessage()
        );
    }

    private String extractFieldName(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        JsonMappingException.Reference lastReference = path.get(path.size() - 1);
        return lastReference.getFieldName();
    }
}
