package com.project.doctorService.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class VisitDetails {
    private String appointmentId;
    private HealthCheck healthCheck;
    private String prescription;
}
