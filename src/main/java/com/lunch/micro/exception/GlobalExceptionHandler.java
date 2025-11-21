package com.lunch.micro.exception;

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

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(DomainException e) {
        Map<String, String> error = new HashMap<>();
        String exceptionMessage = e.getMessage() != null ? e.getMessage() : "An error occurred";
        error.put("error", exceptionMessage);
        error.put("message", exceptionMessage);
        error.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> error = new HashMap<>();
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("Validation failed");
        error.put("error", message);
        error.put("message", message);
        error.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        Map<String, String> error = new HashMap<>();
        String message = "Invalid parameter format: " + e.getName() + ". Expected format: " + 
                        (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        if (e.getMessage() != null && e.getMessage().contains("UUID")) {
            message = "Invalid UUID format for parameter: " + e.getName();
        }
        error.put("error", message);
        error.put("message", message);
        error.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, String>> handleHttpClientErrorException(HttpClientErrorException e) {
        Map<String, String> error = new HashMap<>();
        String errorMessage = e.getMessage();
        if (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty()) {
            errorMessage = e.getResponseBodyAsString();
        }
        error.put("error", errorMessage);
        error.put("message", errorMessage);
        error.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(e.getStatusCode())
                .body(error);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, String>> handleHttpServerErrorException(HttpServerErrorException e) {
        Map<String, String> error = new HashMap<>();
        String errorMessage = e.getMessage();
        if (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty()) {
            errorMessage = e.getResponseBodyAsString();
        }
        error.put("error", errorMessage);
        error.put("message", errorMessage);
        error.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(e.getStatusCode())
                .body(error);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleResourceAccessException(ResourceAccessException e) {
        Map<String, String> error = new HashMap<>();
        String exceptionMessage = e.getMessage() != null ? e.getMessage() : "Service unavailable";
        String fullMessage = "Service unavailable: " + exceptionMessage;
        error.put("error", fullMessage);
        error.put("message", fullMessage);
        error.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        Map<String, String> error = new HashMap<>();
        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        // If there's a cause, include it for more context
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            errorMessage += " - Cause: " + e.getCause().getMessage();
        }
        error.put("error", errorMessage);
        error.put("message", errorMessage);
        error.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

}
