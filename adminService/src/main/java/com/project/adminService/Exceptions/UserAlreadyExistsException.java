package com.project.adminService.Exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() {
        super("User Already exists with Email");
    }
}
