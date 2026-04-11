package com.project.userService.Exceptions;

public class UserAlreadyExistsException extends Exception {
    public UserAlreadyExistsException(String identifier) {
        super("User already exists with email: " + identifier);
    }
}
