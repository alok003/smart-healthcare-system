package com.project.appointmentService.Exceptions;

public class UnAuthorizedException extends Exception {
    public UnAuthorizedException() {
        super("Credentials not Validated for this endpoint");
    }
}
