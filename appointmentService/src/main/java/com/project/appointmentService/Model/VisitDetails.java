package com.project.appointmentService.Model;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class VisitDetails {
    private String appointmentId;
    @Valid
    private HealthCheck healthCheck;
    private String prescription;
}
