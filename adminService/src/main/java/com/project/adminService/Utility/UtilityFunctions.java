package com.project.adminService.Utility;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Model.RequestRoleDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UtilityFunctions {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public RequestRole cnvBeanToEntity(RequestRoleDto requestRoleDto) {
        RequestRole requestRole = new RequestRole();
        BeanUtils.copyProperties(requestRoleDto, requestRole);
        return requestRole;
    }

    public RequestRoleDto cnvEntityToBean(RequestRole requestRole) {
        RequestRoleDto requestRoleDto = new RequestRoleDto();
        BeanUtils.copyProperties(requestRole, requestRoleDto);
        return requestRoleDto;
    }

    public Boolean validateRequestAdmin(String email, String role) {
        return role != null && role.equals("ADMIN") && validateEmail(email);
    }

    public static <T> Map<String, Object> cnvDtoToMap(T dto) {
        return objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {});
    }

    public static <T> T cnvMapToDto(Map<String, Object> map, Class<T> clazz) {
        return objectMapper.convertValue(map, clazz);
    }

    private boolean validateEmail(String email) {
        return email != null && email.contains("@");
    }
}
