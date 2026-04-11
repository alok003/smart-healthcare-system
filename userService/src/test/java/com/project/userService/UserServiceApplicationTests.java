package com.project.userService;

import com.project.userService.Utility.LogUtil;
import com.project.userService.Utility.UtilityFunctions;
import com.project.userService.Entity.User;
import com.project.userService.Model.UserModel;
import com.project.userService.Model.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceApplicationTests {

    private final UtilityFunctions utilityFunction = new UtilityFunctions();

    @Test
    void cnvBeanToEntity_mapsFieldsCorrectly() {
        UserModel model = new UserModel();
        model.setUserEmail("test@example.com");
        model.setUserName("Test User");
        model.setUserAge(25);
        model.setUserPassword("password123");

        User user = utilityFunction.cnvBeanToEntity(model);

        assertEquals("test@example.com", user.getUserEmail());
        assertEquals("Test User", user.getUserName());
        assertEquals(25, user.getUserAge());
    }

    @Test
    void cnvEntityToBean_mapsFieldsCorrectly() {
        User user = new User();
        user.setUserEmail("test@example.com");
        user.setUserName("Test User");
        user.setUserAge(25);
        user.setUserRole(UserRole.USER);

        UserModel model = utilityFunction.cnvEntityToBean(user);

        assertEquals("test@example.com", model.getUserEmail());
        assertEquals("Test User", model.getUserName());
        assertEquals(25, model.getUserAge());
    }

    @Test
    void validateRequestAdmin_validAdmin_returnsTrue() {
        assertTrue(utilityFunction.validateRequestAdmin("admin@example.com", "ADMIN"));
    }

    @Test
    void validateRequestAdmin_wrongRole_returnsFalse() {
        assertFalse(utilityFunction.validateRequestAdmin("admin@example.com", "USER"));
    }

    @Test
    void validateRequestAdmin_nullEmail_returnsFalse() {
        assertFalse(utilityFunction.validateRequestAdmin(null, "ADMIN"));
    }

    @Test
    void validateRequestUser_userRole_returnsTrue() {
        assertTrue(utilityFunction.validateRequestUser("user@example.com", "USER"));
    }

    @Test
    void validateRequestUser_doctorRole_returnsTrue() {
        assertTrue(utilityFunction.validateRequestUser("doc@example.com", "DOCTOR"));
    }

    @Test
    void validateRequestUser_patientRole_returnsTrue() {
        assertTrue(utilityFunction.validateRequestUser("pat@example.com", "PATIENT"));
    }

    @Test
    void validateRequestUser_adminRole_returnsFalse() {
        assertFalse(utilityFunction.validateRequestUser("admin@example.com", "ADMIN"));
    }

    @Test
    void cnvDtoToMap_convertsCorrectly() {
        UserModel model = new UserModel();
        model.setUserEmail("test@example.com");
        model.setUserName("Test");

        Map<String, Object> map = UtilityFunction.cnvDtoToMap(model);

        assertEquals("test@example.com", map.get("userEmail"));
        assertEquals("Test", map.get("userName"));
    }

    @Test
    void logUtil_masksPassword() {
        UserModel model = new UserModel();
        model.setUserEmail("test@example.com");
        model.setUserPassword("secret123");

        String json = LogUtil.toJson(model);

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
}
