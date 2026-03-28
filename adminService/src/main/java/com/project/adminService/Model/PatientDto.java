package com.project.adminService.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class PatientDto {
    private String name;
    private String email;
    private LocalDate dateOfBirth;
    private Gender gender;
}
