package com.project.notificationService.Model;

import lombok.Getter;

@Getter
public enum Subjects {
    WELCOME("Welcome to Smart Healthcare System"),
    ROLE_APPROVED("Your Role Request Has Been Approved - Smart Healthcare System"),
    ROLE_DECLINED("Your Role Request Has Been Declined - Smart Healthcare System"),
    APPOINTMENT_BOOKED("Appointment Confirmation - Smart Healthcare System"),
    APPOINTMENT_CANCELLED("Appointment Cancellation Notice - Smart Healthcare System"),
    APPOINTMENT_COMPLETED("Your Visit Summary - Smart Healthcare System"),
    PRESCRIPTION_READY("Your Prescription is Ready - Smart Healthcare System"),
    DOCTOR_DAILY_SCHEDULE("Your Appointments for Tomorrow - Smart Healthcare System");

    private final String subject;

    Subjects(String subject) {
        this.subject = subject;
    }
}
