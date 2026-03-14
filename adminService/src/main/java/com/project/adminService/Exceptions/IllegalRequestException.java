package com.project.adminService.Exceptions;

public class IllegalRequestException extends RuntimeException {
    public IllegalRequestException() {
        super("Illegal request exception ,Please review your request and try again");
    }
}
