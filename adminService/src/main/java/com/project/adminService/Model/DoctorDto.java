package com.project.adminService.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.adminService.Model.Specialization;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

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
}
