package com.project.adminService;

import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Model.RequestRoleDto;
import com.project.adminService.Model.Status;
import com.project.adminService.Model.UserRole;
import com.project.adminService.Utility.LogUtil;
import com.project.adminService.Utility.UtilityFunctions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceApplicationTests {

    private final UtilityFunctions utilityFunctions = new UtilityFunctions();

    @Test
    void cnvBeanToEntity_mapsFieldsCorrectly() {
        RequestRoleDto dto = new RequestRoleDto();
        dto.setUserEmail("test@example.com");
        dto.setUserRole(UserRole.DOCTOR);
        dto.setRequestStatus(Status.PENDING);

        RequestRole entity = utilityFunctions.cnvBeanToEntity(dto);

        assertEquals("test@example.com", entity.getUserEmail());
        assertEquals(UserRole.DOCTOR, entity.getUserRole());
        assertEquals(Status.PENDING, entity.getRequestStatus());
    }

    @Test
    void cnvEntityToBean_mapsFieldsCorrectly() {
        RequestRole entity = new RequestRole();
        entity.setUserEmail("test@example.com");
        entity.setUserRole(UserRole.ADMIN);
        entity.setRequestStatus(Status.APPROVED);

        RequestRoleDto dto = utilityFunctions.cnvEntityToBean(entity);

        assertEquals("test@example.com", dto.getUserEmail());
        assertEquals(UserRole.ADMIN, dto.getUserRole());
        assertEquals(Status.APPROVED, dto.getRequestStatus());
    }

    @Test
    void validateRequestAdmin_validAdmin_returnsTrue() {
        assertTrue(utilityFunctions.validateRequestAdmin("admin@example.com", "ADMIN"));
    }

    @Test
    void validateRequestAdmin_wrongRole_returnsFalse() {
        assertFalse(utilityFunctions.validateRequestAdmin("admin@example.com", "USER"));
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
        RequestRoleDto dto = new RequestRoleDto();
        dto.setUserEmail("test@example.com");
        dto.setUserRole(UserRole.DOCTOR);

        Map<String, Object> map = UtilityFunctions.cnvDtoToMap(dto);

        assertEquals("test@example.com", map.get("userEmail"));
        assertEquals("DOCTOR", map.get("userRole"));
    }

    @Test
    void cnvMapToDto_convertsCorrectly() {
        RequestRoleDto original = new RequestRoleDto();
        original.setUserEmail("test@example.com");
        original.setUserRole(UserRole.PATIENT);

        Map<String, Object> map = UtilityFunctions.cnvDtoToMap(original);
        RequestRoleDto result = UtilityFunctions.cnvMapToDto(map, RequestRoleDto.class);

        assertEquals("test@example.com", result.getUserEmail());
        assertEquals(UserRole.PATIENT, result.getUserRole());
    }

    @Test
    void logUtil_masksPassword() {
        RequestRoleDto dto = new RequestRoleDto();
        dto.setUserEmail("test@example.com");

        String json = LogUtil.toJson(dto);

        assertNotNull(json);
        assertTrue(json.contains("test@example.com"));
    }

    @Test
    void logUtil_truncatesLongPayload() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) sb.append("a");
        String result = LogUtil.toJson(sb.toString());
        assertTrue(result.contains("[truncated]"));
    }
}
