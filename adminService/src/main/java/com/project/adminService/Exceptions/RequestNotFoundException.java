package com.project.adminService.Exceptions;

public class RequestNotFoundException extends Exception {
    public RequestNotFoundException() {
        super("Request Not Found or Already Action is taken Exception");
    }
}
