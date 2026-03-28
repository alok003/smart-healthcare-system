package com.project.patientService.Exception;

public class PatientAlreadyExistsException extends Exception {
    public PatientAlreadyExistsException(String email) {
        super("Patient with email " + email + " already exists.");
    }
}
