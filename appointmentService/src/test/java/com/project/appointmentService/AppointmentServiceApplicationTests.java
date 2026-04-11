package com.project.appointmentService;

import com.project.appointmentService.Entity.Appointment;
import com.project.appointmentService.Model.AppointmentDto;
import com.project.appointmentService.Model.Status;
import com.project.appointmentService.Utility.LogUtil;
import com.project.appointmentService.Utility.UtilityFunctions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentServiceApplicationTests {

    private final UtilityFunctions utilityFunctions = new UtilityFunctions();

    @Test
    void cnvDtoToEntity_mapsFieldsCorrectly() {
        AppointmentDto dto = new AppointmentDto();
        dto.setPatientId("patient@example.com");
        dto.setDoctorId("doctor@example.com");
        dto.setStatus(Status.UPCOMING);
        dto.setDate(LocalDate.now());

        Appointment entity = utilityFunctions.cnvDtoToEntity(dto);

        assertEquals("patient@example.com", entity.getPatientId());
        assertEquals("doctor@example.com", entity.getDoctorId());
        assertEquals(Status.UPCOMING, entity.getStatus());
    }

    @Test
    void cnvEntityToDto_mapsFieldsCorrectly() {
        Appointment entity = new Appointment();
        entity.setPatientId("patient@example.com");
        entity.setDoctorId("doctor@example.com");
        entity.setStatus(Status.VISITED);
        entity.setDate(LocalDate.now());

        AppointmentDto dto = utilityFunctions.cnvEntityToDto(entity);

        assertEquals("patient@example.com", dto.getPatientId());
        assertEquals("doctor@example.com", dto.getDoctorId());
        assertEquals(Status.VISITED, dto.getStatus());
    }

    @Test
    void validateRequestAdmin_validAdmin_returnsTrue() {
        assertTrue(utilityFunctions.validateRequestAdmin("admin@example.com", "ADMIN"));
    }

    @Test
    void validateRequestAdmin_wrongRole_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestAdmin("admin@example.com", "PATIENT"));
    }

    @Test
    void validateRequestAdmin_nullEmail_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestAdmin(null, "ADMIN"));
    }

    @Test
    void validateRequestAdmin_nullRole_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestAdmin("admin@example.com", null));
    }

    @Test
    void validateRequestAdmin_invalidEmail_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestAdmin("notanemail", "ADMIN"));
    }

    @Test
    void cnvDtoToMap_convertsCorrectly() {
        AppointmentDto dto = new AppointmentDto();
        dto.setPatientId("patient@example.com");
        dto.setStatus(Status.UPCOMING);

        Map<String, Object> map = UtilityFunctions.cnvDtoToMap(dto);

        assertEquals("patient@example.com", map.get("patientId"));
        assertEquals("UPCOMING", map.get("status"));
    }

    @Test
    void logUtil_masksPassword() {
        String json = LogUtil.toJson(Map.of("userPassword", "secret123", "email", "test@example.com"));
        assertTrue(json.contains("***"));
        assertFalse(json.contains("secret123"));
    }

    @Test
    void logUtil_truncatesLongPayload() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) sb.append("a");
        String result = LogUtil.toJson(sb.toString());
        assertTrue(result.contains("[truncated]"));
    }

    @Test
    void logUtil_nullObject_returnsNull() {
        assertEquals("null", LogUtil.toJson(null));
    }
}
