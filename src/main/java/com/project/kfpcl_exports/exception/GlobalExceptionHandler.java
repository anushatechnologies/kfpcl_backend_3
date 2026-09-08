package com.project.kfpcl_exports.exception;

import com.project.kfpcl_exports.dto.AuthDTOs.GenericResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@RestControllerAdvice
@Component("mainGlobalExceptionHandler")
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                GenericResponse.builder().success(false).message(ex.getMessage()).build()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<GenericResponse> handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (msg.contains("Too many OTP requests") || msg.contains("Please wait")) {
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else if (msg.contains("already registered") || msg.contains("already exists")) {
            status = HttpStatus.CONFLICT;
        }
        return ResponseEntity.status(status).body(
                GenericResponse.builder().success(false).message(ex.getMessage()).build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                GenericResponse.builder().success(false).message(errorMsg).build()
        );
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<GenericResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                GenericResponse.builder().success(false).message("Resource not found: " + ex.getResourcePath()).build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                GenericResponse.builder().success(false).message(ex.getMessage()).build()
        );
    }
}
