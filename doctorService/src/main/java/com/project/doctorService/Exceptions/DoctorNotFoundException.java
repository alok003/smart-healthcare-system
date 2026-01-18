package com.project.doctorService.Exceptions;

public class DoctorNotFoundException extends RuntimeException {
    public DoctorNotFoundException() {
        super("Doctor Not found Exception");
    }
}
