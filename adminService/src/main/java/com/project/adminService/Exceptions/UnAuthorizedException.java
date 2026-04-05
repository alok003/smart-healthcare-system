package com.project.adminService.Exceptions;

public class UnAuthorizedException extends Exception {
    public UnAuthorizedException(String email) {
        super("Credentials not validated for: " + email);
    }
}
