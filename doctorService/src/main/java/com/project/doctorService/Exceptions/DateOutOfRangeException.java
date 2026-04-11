package com.project.doctorService.Exceptions;

public class DateOutOfRangeException extends Exception {
    public DateOutOfRangeException() {
        super("Selected Date is out of range. Please select a date within the allowed range.");
    }
}
