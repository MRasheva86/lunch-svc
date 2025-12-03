package com.lunch.micro.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleDomainException_WithMessage_ReturnsBadRequest() {

        String errorMessage = "Order not found";
        DomainException exception = new DomainException(errorMessage);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleDomainException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo(errorMessage);
        assertThat(response.getBody().get("message")).isEqualTo(errorMessage);
        assertThat(response.getBody().get("timestamp")).isNotNull();
        assertThat(Instant.parse(response.getBody().get("timestamp"))).isNotNull();
    }

    @Test
    void handleDomainException_WithNullMessage_ReturnsDefaultMessage() {

        DomainException exception = mock(DomainException.class);
        when(exception.getMessage()).thenReturn(null);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleDomainException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("An error occurred");
        assertThat(response.getBody().get("message")).isEqualTo("An error occurred");
        verify(exception, atLeastOnce()).getMessage();
    }

    @Test
    void handleDomainException_WithEmptyMessage_ReturnsEmptyString() {

        DomainException exception = mock(DomainException.class);
        when(exception.getMessage()).thenReturn("");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleDomainException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("");
        assertThat(response.getBody().get("message")).isEqualTo("");
        verify(exception, atLeastOnce()).getMessage();
    }

    @Test
    void handleValidationException_SingleFieldError_ReturnsBadRequest() {

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("lunchOrderRequest", "quantity", "Quantity must be greater than zero");
        List<FieldError> fieldErrors = List.of(fieldError);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        String errorMessage = response.getBody().get("error");
        assertThat(errorMessage).contains("quantity");
        assertThat(errorMessage).contains("Quantity must be greater than zero");
        assertThat(response.getBody().get("message")).isEqualTo(errorMessage);
        
        verify(exception, times(1)).getBindingResult();
        verify(bindingResult, times(1)).getFieldErrors();
    }

    @Test
    void handleValidationException_MultipleFieldErrors_ReturnsBadRequest() {

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("lunchOrderRequest", "quantity", "Quantity must be greater than zero");
        FieldError fieldError2 = new FieldError("lunchOrderRequest", "parentId", "Parent ID is required");
        FieldError fieldError3 = new FieldError("lunchOrderRequest", "meal", "Meal is required");
        List<FieldError> fieldErrors = List.of(fieldError1, fieldError2, fieldError3);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        String errorMessage = response.getBody().get("error");
        assertThat(errorMessage).contains("quantity");
        assertThat(errorMessage).contains("parentId");
        assertThat(errorMessage).contains("meal");
        assertThat(errorMessage).contains(";");
        assertThat(errorMessage.split(";").length).isEqualTo(3);
        
        verify(exception, times(1)).getBindingResult();
        verify(bindingResult, times(1)).getFieldErrors();
    }

    @Test
    void handleValidationException_EmptyFieldErrors_ReturnsDefaultMessage() {

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> fieldErrors = new ArrayList<>();

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Validation failed");
        
        verify(exception, times(1)).getBindingResult();
        verify(bindingResult, times(1)).getFieldErrors();
    }

    @Test
    void handleTypeMismatchException_WithNullRequiredType_ReturnsBadRequest() {

        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        String parameterName = "orderId";

        when(exception.getName()).thenReturn(parameterName);
        when(exception.getRequiredType()).thenReturn(null);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleTypeMismatchException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        String errorMessage = response.getBody().get("error");
        assertThat(errorMessage).contains("Invalid parameter format");
        assertThat(errorMessage).contains(parameterName);
        assertThat(errorMessage).contains("unknown");
        
        verify(exception, times(1)).getName();
        verify(exception, times(1)).getRequiredType();
    }

    @Test
    void handleHttpClientErrorException_ReturnsCorrectStatusCode() {

        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        String errorMessage = "400 Bad Request: Invalid request";
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;
        
        when(exception.getMessage()).thenReturn(errorMessage);
        when(exception.getStatusCode()).thenReturn(statusCode);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleHttpClientErrorException(exception);

        assertThat(response.getStatusCode()).isEqualTo(statusCode);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo(errorMessage);
        assertThat(response.getBody().get("message")).isEqualTo(errorMessage);
        
        verify(exception, atLeastOnce()).getMessage();
        verify(exception, atLeastOnce()).getStatusCode();
    }

    @Test
    void handleHttpClientErrorException_WithNullMessage_ReturnsNullErrorMessage() {

        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        HttpStatus statusCode = HttpStatus.NOT_FOUND;
        
        when(exception.getMessage()).thenReturn(null);
        when(exception.getStatusCode()).thenReturn(statusCode);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleHttpClientErrorException(exception);

        assertThat(response.getStatusCode()).isEqualTo(statusCode);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isNull();
        
        verify(exception, atLeastOnce()).getMessage();
        verify(exception, atLeastOnce()).getStatusCode();
    }

    @Test
    void handleHttpServerErrorException_ReturnsCorrectStatusCode() {

        HttpServerErrorException exception = mock(HttpServerErrorException.class);
        String errorMessage = "500 Internal Server Error: Database connection failed";
        HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        
        when(exception.getMessage()).thenReturn(errorMessage);
        when(exception.getStatusCode()).thenReturn(statusCode);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleHttpServerErrorException(exception);

        assertThat(response.getStatusCode()).isEqualTo(statusCode);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo(errorMessage);
        assertThat(response.getBody().get("message")).isEqualTo(errorMessage);
        
        verify(exception, atLeastOnce()).getMessage();
        verify(exception, atLeastOnce()).getStatusCode();
    }

    @Test
    void handleHttpServerErrorException_WithDifferentStatusCode_ReturnsCorrectStatusCode() {

        HttpServerErrorException exception = mock(HttpServerErrorException.class);
        String errorMessage = "503 Service Unavailable";
        HttpStatus statusCode = HttpStatus.SERVICE_UNAVAILABLE;
        
        when(exception.getMessage()).thenReturn(errorMessage);
        when(exception.getStatusCode()).thenReturn(statusCode);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleHttpServerErrorException(exception);

        assertThat(response.getStatusCode()).isEqualTo(statusCode);
        assertThat(response.getBody()).isNotNull();
        
        verify(exception, atLeastOnce()).getMessage();
        verify(exception, atLeastOnce()).getStatusCode();
    }

    @Test
    void handleResourceAccessException_WithMessage_ReturnsServiceUnavailable() {

        ResourceAccessException exception = mock(ResourceAccessException.class);
        String errorMessage = "Connection timeout after 30 seconds";
        
        when(exception.getMessage()).thenReturn(errorMessage);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleResourceAccessException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        String responseError = response.getBody().get("error");
        assertThat(responseError).contains("Service unavailable");
        assertThat(responseError).contains(errorMessage);
        
        verify(exception, atLeastOnce()).getMessage();
    }

    @Test
    void handleResourceAccessException_WithNullMessage_ReturnsDefaultMessage() {

        ResourceAccessException exception = mock(ResourceAccessException.class);
        when(exception.getMessage()).thenReturn(null);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleResourceAccessException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Service unavailable: Service unavailable");
        
        verify(exception, atLeastOnce()).getMessage();
    }

    @Test
    void handleGenericException_WithMessage_ReturnsInternalServerError() {

        String errorMessage = "Unexpected error occurred";
        RuntimeException exception = new RuntimeException(errorMessage);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo(errorMessage);
        assertThat(response.getBody().get("message")).isEqualTo(errorMessage);
    }

    @Test
    void handleGenericException_WithNullMessage_ReturnsClassName() {

        IOException exception = new IOException((String) null);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("IOException");
    }

    @Test
    void handleGenericException_WithCause_ReturnsErrorWithCause() {

        String causeMessage = "Root cause: Database connection lost";
        RuntimeException cause = new RuntimeException(causeMessage);
        RuntimeException exception = new RuntimeException("Top level error", cause);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        String errorMessage = response.getBody().get("error");
        assertThat(errorMessage).contains("Top level error");
        assertThat(errorMessage).contains("Cause: " + causeMessage);
    }

    @Test
    void handleGenericException_WithNullCauseMessage_ReturnsErrorWithoutCause() {

        RuntimeException cause = new RuntimeException((String) null);
        RuntimeException exception = new RuntimeException("Top level error", cause);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Top level error");
        assertThat(response.getBody().get("error")).doesNotContain("Cause:");
    }

    @Test
    void handleGenericException_WithNullCause_ReturnsErrorWithoutCause() {

        RuntimeException exception = new RuntimeException("Error without cause");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Error without cause");
        assertThat(response.getBody().get("error")).doesNotContain("Cause:");
    }

    @Test
    void handleDomainException_IncludesTimestamp() {

        String errorMessage = "Test error";
        DomainException exception = new DomainException(errorMessage);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleDomainException(exception);

        assertThat(response.getBody()).isNotNull();
        String timestamp = response.getBody().get("timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(Instant.parse(timestamp)).isNotNull();

        Instant parsedTimestamp = Instant.parse(timestamp);
        assertThat(parsedTimestamp).isAfter(Instant.now().minusSeconds(5));
        assertThat(parsedTimestamp).isBefore(Instant.now().plusSeconds(1));
    }

    @Test
    void createErrorResponse_ContainsAllRequiredFields() {

        String errorMessage = "Test error message";
        DomainException exception = new DomainException(errorMessage);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleDomainException(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("error");
        assertThat(response.getBody()).containsKey("message");
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().size()).isEqualTo(3);
    }

    @Test
    void handleDifferentExceptionTypes_Correctly() {

        DomainException domainException = new DomainException("Domain error");
        RuntimeException runtimeException = new RuntimeException("Runtime error");

        ResponseEntity<Map<String, String>> domainResponse = globalExceptionHandler.handleDomainException(domainException);
        ResponseEntity<Map<String, String>> genericResponse = globalExceptionHandler.handleGenericException(runtimeException);

        assertThat(domainResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(genericResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
