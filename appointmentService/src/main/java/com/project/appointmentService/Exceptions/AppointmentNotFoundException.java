package com.project.appointmentService.Exceptions;

public class AppointmentNotFoundException extends Exception{
    public AppointmentNotFoundException(String id) {
        super("Appointment Not found Exception with given id:"+id);
    }
}
