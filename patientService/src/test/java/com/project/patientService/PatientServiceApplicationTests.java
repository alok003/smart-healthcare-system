package com.project.patientService;

import com.project.patientService.Entity.Patient;
import com.project.patientService.Model.Gender;
import com.project.patientService.Model.PatientDto;
import com.project.patientService.Utility.LogUtil;
import com.project.patientService.Utility.UtilityFunctions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PatientServiceApplicationTests {

    private final UtilityFunctions utilityFunctions = new UtilityFunctions();

    @Test
    void convertToPatient_mapsFieldsCorrectly() {
        PatientDto dto = new PatientDto();
        dto.setEmail("patient@example.com");
        dto.setName("John Doe");
        dto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        dto.setGender(Gender.MALE);

        Patient patient = utilityFunctions.convertToPatient(dto);

        assertEquals("patient@example.com", patient.getEmail());
        assertEquals("John Doe", patient.getName());
        assertEquals(LocalDate.of(1990, 1, 1), patient.getDateOfBirth());
    }

    @Test
    void convertToPatient_initializesCollections() {
        PatientDto dto = new PatientDto();
        dto.setEmail("patient@example.com");

        Patient patient = utilityFunctions.convertToPatient(dto);

        assertNotNull(patient.getAppointmentList());
        assertNotNull(patient.getVitalsFlow());
    }

    @Test
    void convertToPatientDto_mapsFieldsCorrectly() {
        Patient patient = new Patient();
        patient.setEmail("patient@example.com");
        patient.setName("Jane Doe");
        patient.setGender(Gender.FEMALE);

        PatientDto dto = utilityFunctions.convertToPatientDto(patient);

        assertEquals("patient@example.com", dto.getEmail());
        assertEquals("Jane Doe", dto.getName());
        assertEquals(Gender.FEMALE, dto.getGender());
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
    void validateRequestPatient_validPatient_returnsTrue() {
        assertTrue(utilityFunctions.validateRequestPatient("patient@example.com", "PATIENT"));
    }

    @Test
    void validateRequestPatient_wrongRole_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestPatient("patient@example.com", "DOCTOR"));
    }

    @Test
    void validateRequestPatient_invalidEmail_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestPatient("notanemail", "PATIENT"));
    }

    @Test
    void cnvDtoToMap_convertsCorrectly() {
        PatientDto dto = new PatientDto();
        dto.setEmail("patient@example.com");
        dto.setName("John");

        Map<String, Object> map = UtilityFunctions.cnvDtoToMap(dto);

        assertEquals("patient@example.com", map.get("email"));
        assertEquals("John", map.get("name"));
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
