package com.project.patientService.Exception;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String id) {
        super("Patient not found with given id:"+id);
    }
}
