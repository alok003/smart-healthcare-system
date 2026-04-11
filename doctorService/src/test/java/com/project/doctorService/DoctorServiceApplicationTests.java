package com.project.doctorService;

import com.project.doctorService.Entity.Doctor;
import com.project.doctorService.Model.DoctorDto;
import com.project.doctorService.Model.Gender;
import com.project.doctorService.Utility.LogUtil;
import com.project.doctorService.Utility.UtilityFunctions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DoctorServiceApplicationTests {

    private final UtilityFunctions utilityFunctions = new UtilityFunctions();

    @Test
    void cnvBeanToEntity_mapsFieldsCorrectly() {
        DoctorDto dto = new DoctorDto();
        dto.setEmail("doctor@example.com");
        dto.setGender(Gender.MALE);
        dto.setLicenseNumber("LIC-001");

        Doctor entity = utilityFunctions.cnvBeanToEntity(dto);

        assertEquals("doctor@example.com", entity.getEmail());
        assertEquals(Gender.MALE, entity.getGender());
        assertEquals("LIC-001", entity.getLicenseNumber());
    }

    @Test
    void cnvEntityToBean_mapsFieldsCorrectly() {
        Doctor entity = new Doctor();
        entity.setEmail("doctor@example.com");
        entity.setGender(Gender.FEMALE);
        entity.setContactNumber("9999999999");

        DoctorDto dto = utilityFunctions.cnvEntityToBean(entity);

        assertEquals("doctor@example.com", dto.getEmail());
        assertEquals(Gender.FEMALE, dto.getGender());
        assertEquals("9999999999", dto.getContactNumber());
    }

    @Test
    void validateRequestAdmin_validAdmin_returnsTrue() {
        assertTrue(utilityFunctions.validateRequestAdmin("admin@example.com", "ADMIN"));
    }

    @Test
    void validateRequestAdmin_wrongRole_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestAdmin("admin@example.com", "DOCTOR"));
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
    void validateRequestDoctor_validDoctor_returnsTrue() {
        assertTrue(utilityFunctions.validateRequestDoctor("doctor@example.com", "DOCTOR"));
    }

    @Test
    void validateRequestDoctor_wrongRole_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestDoctor("doctor@example.com", "PATIENT"));
    }

    @Test
    void validateRequestDoctor_invalidEmail_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestDoctor("notanemail", "DOCTOR"));
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
