package com.project.patientService.Exception;

public class UnAuthorizedException extends Exception {
    public UnAuthorizedException() {
        super("Credentials not Validated for this endpoint");
    }
}
