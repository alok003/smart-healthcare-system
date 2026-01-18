package com.project.userService.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.userService.Model.Specialization;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.List;
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorDto {
    private String id;
    private String email;
    private Gender gender;
    private List<Specialization> specializations;
    private String licenseNumber;
    private String contactNumber;
    private String overview;
    private Bookings bookings;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedBy
    private String lastModifiedBy;
}
