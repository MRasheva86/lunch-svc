package com.lunch.micro.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, String> createErrorResponse(String message, HttpStatus status) {

        Map<String, String> error = new HashMap<>();

        error.put("error", message);
        error.put("message", message);
        error.put("timestamp", Instant.now().toString());

        return error;
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(DomainException e) {

        String exceptionMessage = e.getMessage() != null ? e.getMessage() : "An error occurred";

        logger.warn("DomainException occurred: {}", exceptionMessage);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(exceptionMessage, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {

        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("Validation failed");

        logger.warn("Validation exception occurred: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(message, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {

        String message = "Invalid parameter format: " + e.getName() + ". Expected format: " +
                        (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");

        logger.warn("Type mismatch exception occurred: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(message, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, String>> handleHttpClientErrorException(HttpClientErrorException e) {

        String errorMessage = e.getMessage();

        logger.error("HTTP client error exception occurred: {} - Status: {}", errorMessage, e.getStatusCode());

        return ResponseEntity.status(e.getStatusCode())
                .body(createErrorResponse(errorMessage, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, String>> handleHttpServerErrorException(HttpServerErrorException e) {

        String errorMessage = e.getMessage();

        logger.error("HTTP server error exception occurred: {} - Status: {}", errorMessage, e.getStatusCode());

        return ResponseEntity.status(e.getStatusCode())
                .body(createErrorResponse(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleResourceAccessException(ResourceAccessException e) {

        String exceptionMessage = e.getMessage() != null ? e.getMessage() : "Service unavailable";
        String fullMessage = "Service unavailable: " + exceptionMessage;

        logger.error("Resource access exception occurred: {}", exceptionMessage);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createErrorResponse(fullMessage, HttpStatus.SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {

        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

        if (e.getCause() != null && e.getCause().getMessage() != null) {
            errorMessage += " - Cause: " + e.getCause().getMessage();
        }

        logger.error("Unexpected exception occurred: {} - {}", e.getClass().getSimpleName(), errorMessage, e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
