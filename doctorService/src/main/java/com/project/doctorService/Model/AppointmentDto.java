package com.project.doctorService.Model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class AppointmentDto {
    private String id;
    private String patientId;
    private String doctorId;
    @Field(targetType = FieldType.STRING)
    private Status status;
    private String subject;
    private String description;
    private LocalDate date;
    private VisitDetails visitDetails;
}
