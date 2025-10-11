package com.project.userService.Model;

import com.project.userService.Model.Specialization;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

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
}
