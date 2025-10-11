package com.project.doctorService.Exceptions;

public class RequestNotFoundException extends Exception {
    public RequestNotFoundException() {
        super("Request Not Found Exception");
    }
}
