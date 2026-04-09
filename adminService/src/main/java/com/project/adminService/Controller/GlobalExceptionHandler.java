package com.project.adminService.Controller;

import com.project.adminService.Exceptions.IllegalRequestException;
import com.project.adminService.Exceptions.RequestNotFoundException;
import com.project.adminService.Exceptions.UnAuthorizedException;
import com.project.adminService.Model.ApiExceptionResponseTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleRequestNotFound(RequestNotFoundException ex) {
        log.warn("action=EXCEPTION_HANDLED exception=RequestNotFoundException reason={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(UnAuthorizedException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleUnAuthorized(UnAuthorizedException ex) {
        log.warn("action=EXCEPTION_HANDLED exception=UnAuthorizedException reason={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(IllegalRequestException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleIllegalRequest(IllegalRequestException ex) {
        log.warn("action=EXCEPTION_HANDLED exception=IllegalRequestException reason={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleValidation(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        log.warn("action=EXCEPTION_HANDLED exception=ValidationException reason={}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(errorMsg).build());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleRuntimeException(RuntimeException ex) {
        log.error("action=EXCEPTION_HANDLED exception=RuntimeException reason={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleException(Exception ex) {
        log.error("action=EXCEPTION_HANDLED exception={} reason={}", ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }
}
