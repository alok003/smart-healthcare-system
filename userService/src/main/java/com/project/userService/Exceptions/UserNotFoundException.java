package com.project.userService.Exceptions;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(String identifier) {
        super("User not found with id: " + identifier);
    }
}
