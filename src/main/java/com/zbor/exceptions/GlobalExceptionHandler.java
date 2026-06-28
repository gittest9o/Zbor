package com.zbor.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ZborException.class)
    public ResponseEntity<Map<String, String>> handleZbor(ZborException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EventNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral (Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal Server Error"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (a, b) -> a
                ));

        return ResponseEntity.badRequest()
                .body(Map.of("errors", errors));
    }
}