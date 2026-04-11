package com.project.patientService.Model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class AppointmentDto {
    private String id;
    private String patientId;
    @NotNull(message = "doctorId is required")
    private String doctorId;
    private Status status;
    private String subject;
    private String description;
    @NotNull(message = "date is required")
    private LocalDate date;
    private VisitDetails visitDetails;
}
