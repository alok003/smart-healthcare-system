package com.project.adminService.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class RequestRoleDto {
    private String id;
    private String userEmail;
    private UserRole userRole;
    private Map<String,Object> attributes;
}
