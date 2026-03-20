package com.project.appointmentService.Utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.appointmentService.Entity.Appointment;
import com.project.appointmentService.Model.AppointmentDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class UtilityFunctions {

    private static final ObjectMapper objectMapper=new ObjectMapper();

    public Boolean validateRequestAdmin(String email, String role) {
        return Objects.equals(role, "ADMIN") && validateEmail(email);
    }

    public AppointmentDto cnvEntityToDto(Object entity, Class<AppointmentDto> clazz) {
        AppointmentDto appointmentDto = new AppointmentDto();
        BeanUtils.copyProperties(entity, appointmentDto);
        return appointmentDto;
    }

    public Appointment cnvDtoToEntity(AppointmentDto appointmentDto, Class<Appointment> clazz) {
        Appointment appointment = new Appointment();
        BeanUtils.copyProperties(appointmentDto, appointment);
        return appointment;
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
