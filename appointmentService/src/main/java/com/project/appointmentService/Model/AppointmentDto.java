package com.project.appointmentService.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;
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
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedBy
    private String lastModifiedBy;
}
