package com.project.patientService.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class PatientDto {
    private String id;
    private String email;
    private String name;
    private LocalDate dateOfBirth;
    private Gender gender;
    private List<String> appointmentList;
    private Map<LocalDate, HealthCheck> vitalsFlow;
}
