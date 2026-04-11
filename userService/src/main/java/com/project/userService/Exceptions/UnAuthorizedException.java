package com.project.userService.Exceptions;

public class UnAuthorizedException extends Exception {
    public UnAuthorizedException(String email) {
        super("Unauthorized access attempt by: " + email);
    }
}
