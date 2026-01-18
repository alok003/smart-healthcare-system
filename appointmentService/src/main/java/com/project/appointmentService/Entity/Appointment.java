package com.project.appointmentService.Entity;



import com.project.appointmentService.Model.HealthCheck;
import com.project.appointmentService.Model.Status;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Document(collection = "Appointments")
public class Appointment {
    @Id
    private String id;
    private String patientId;
    private String doctorId;
    @Field(targetType = FieldType.STRING)
    private Status status;
    private String subject;
    private String description;
    private LocalDate date;
    private HealthCheck healthCheck;
    private String prescription;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedBy
    private String lastModifiedBy;
}
