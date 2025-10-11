package com.project.adminService.Entity;

import com.project.adminService.Model.Status;
import com.project.adminService.Model.UserRole;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "Requests")
public class RequestRole {
    @Id
    private String id;
    private String userEmail;
    @Field(targetType = FieldType.STRING)
    private UserRole userRole;
    @Field(targetType = FieldType.STRING)
    private Status requestStatus;
    private Doctor doctor;
    @CreatedDate
    private Instant createdAt;
}
