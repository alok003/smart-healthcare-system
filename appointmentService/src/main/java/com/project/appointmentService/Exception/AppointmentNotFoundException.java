package com.project.appointmentService.Exception;

public class AppointmentNotFoundException extends Exception{
    public AppointmentNotFoundException(String id) {
        super("Appointment Not found Exception with given id:"+id);
    }
}
