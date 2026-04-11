package com.project.adminService.Exceptions;

public class RequestNotFoundException extends Exception {
    public RequestNotFoundException(String id) {
        super("Request not found or action already taken for: " + id);
    }
}
