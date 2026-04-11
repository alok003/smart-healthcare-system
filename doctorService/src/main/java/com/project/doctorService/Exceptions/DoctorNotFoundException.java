package com.project.doctorService.Exceptions;

public class DoctorNotFoundException extends Exception {
    public DoctorNotFoundException() {
        super("Doctor Not found Exception");
    }
}
