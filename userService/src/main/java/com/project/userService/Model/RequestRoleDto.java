package com.project.userService.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestRoleDto {
    private String id;
    private String userEmail;
    private UserRole userRole;
    private Status requestStatus;
    private DoctorDto doctorDto;
}
