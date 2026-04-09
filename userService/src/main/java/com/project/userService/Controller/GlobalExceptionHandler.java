package com.project.userService.Controller;

import com.project.userService.Exceptions.InvalidRequestException;
import com.project.userService.Exceptions.UnAuthorizedException;
import com.project.userService.Exceptions.UserAlreadyExistsException;
import com.project.userService.Exceptions.UserNotFoundException;
import com.project.userService.Model.ApiExceptionResponseTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleUserNotFound(UserNotFoundException ex) {
        log.warn("action=EXCEPTION_HANDLED exception=UserNotFoundException reason={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleInvalidRequest(InvalidRequestException ex) {
        log.warn("action=EXCEPTION_HANDLED exception=InvalidRequestException reason={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(UnAuthorizedException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleUnAuthorized(UnAuthorizedException ex) {
        log.warn("action=EXCEPTION_HANDLED exception=UnAuthorizedException reason={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("action=EXCEPTION_HANDLED exception=UserAlreadyExistsException reason={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleBadCredentials(BadCredentialsException ex) {
        log.warn("action=EXCEPTION_HANDLED exception=BadCredentialsException reason=INVALID_CREDENTIALS");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message("Invalid email or password.").build());
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
