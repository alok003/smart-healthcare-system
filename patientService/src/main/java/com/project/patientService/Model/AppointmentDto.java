package com.project.patientService.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class AppointmentDto {
    private String id;
    private String patientId;
    private String doctorId;
    private Status status;
    private String subject;
    private String description;
    private LocalDate date;
    private VisitDetails visitDetails;
    private Instant createdAt;
    private Instant updatedAt;
    private String lastModifiedBy;
}
