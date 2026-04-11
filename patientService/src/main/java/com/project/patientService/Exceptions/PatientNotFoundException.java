package com.project.patientService.Exceptions;

public class PatientNotFoundException extends Exception {
    public PatientNotFoundException(String id) {
        super("Patient not found with given id:" + id);
    }
}
