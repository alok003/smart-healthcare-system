package com.project.patientService.Entity;

import com.project.patientService.Model.Gender;
import com.project.patientService.Model.HealthCheck;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "Patients")
public class Patient {
    @Id
    private String id;
    private String email;
    private String name;
    private LocalDate dateOfBirth;
    @Field(targetType = FieldType.STRING)
    private Gender gender;
    private List<String> appointmentList;
    private Map<LocalDate, HealthCheck> vitalsFlow;
}
