package com.project.Anusha.exception;

import com.project.Anusha.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RfqException.class)
    public ResponseEntity<ApiResponse<Void>> handleRfqException(RfqException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), ex.getErrorCode()), ex.getStatus());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "NOT_FOUND"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Validation Failed")
                .error("Validation Failed")
                .errorCode("INVALID_REQUEST")
                .fields(fieldErrors)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "RFQ_ACCESS_DENIED"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "INVALID_REQUEST"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
