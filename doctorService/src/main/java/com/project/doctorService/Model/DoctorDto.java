package com.project.doctorService.Model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.List;
@Data
@AllArgsConstructor
@RequiredArgsConstructor
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
