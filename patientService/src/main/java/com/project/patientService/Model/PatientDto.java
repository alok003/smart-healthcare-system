package com.project.patientService.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

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
    @Field(targetType = FieldType.STRING)
    private Gender gender;
    private List<String> appointmentList;
    private Map<LocalDate, HealthCheck> vitalsFlow;
}
