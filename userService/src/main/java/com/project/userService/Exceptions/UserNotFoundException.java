package com.project.userService.Exceptions;

public class UserNotFoundException extends Exception {
    public UserNotFoundException() {
        super("User Not Found Exception");
    }
}
