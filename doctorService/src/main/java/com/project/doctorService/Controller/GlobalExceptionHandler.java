package com.project.doctorService.Controller;

import com.project.doctorService.Exceptions.DoctorNotFoundException;
import com.project.doctorService.Exceptions.RequestNotFoundException;
import com.project.doctorService.Exceptions.UnAuthorizedException;
import com.project.doctorService.Exceptions.UserAlreadyExistsException;
import com.project.doctorService.Model.ApiExceptionResponseTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DoctorNotFoundException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleDoctorNotFound(DoctorNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleRequestNotFound(RequestNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(UnAuthorizedException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleUnAuthorized(UnAuthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleUserAlreadyExists(UserAlreadyExistsException ex) {
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(errorMsg).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiExceptionResponseTemplate> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiExceptionResponseTemplate.builder().timestamp(new Date()).message(ex.getMessage()).build());
    }
}