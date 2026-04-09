package com.project.userService.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userService.Entity.User;
import com.project.userService.Model.*;
import com.project.userService.Service.ExternalServiceClient;
import com.project.userService.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.messaging.Message;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @MockitoBean
    private ExternalServiceClient externalServiceClient;

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_EMAIL = "user@example.com";
    private static final String USER_ROLE = "USER";

    private User buildUser(String email, UserRole role) {
        User user = new User();
        user.setUserId("uid-1");
        user.setUserEmail(email);
        user.setUserPassword(passwordEncoder.encode("password123"));
        user.setUserName("Test User");
        user.setUserAge(25);
        user.setUserRole(role);
        return user;
    }

    // --- getUserId ---

    @Test
    void getUserById_success() throws Exception {
        User user = buildUser(ADMIN_EMAIL, UserRole.ADMIN);
        when(userRepository.findById("uid-1")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/user-service/secure/getUserId/uid-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value(ADMIN_EMAIL));
    }

    @Test
    void getUserById_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/user-service/secure/getUserId/uid-1")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/user-service/secure/getUserId/unknown")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNotFound());
    }

    // --- getEmailId ---

    @Test
    void getUserByEmail_success() throws Exception {
        User user = buildUser("target@example.com", UserRole.USER);
        when(userRepository.findByUserEmail("target@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/user-service/secure/getEmailId/target@example.com")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("target@example.com"));
    }

    @Test
    void getUserByEmail_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/user-service/secure/getEmailId/target@example.com")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- findUserId ---

    @Test
    void findUserById_exists_returnsTrue() throws Exception {
        when(userRepository.existsByUserId("uid-1")).thenReturn(true);

        mockMvc.perform(get("/api/user-service/secure/findUserId/uid-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void findUserById_notExists_returnsFalse() throws Exception {
        when(userRepository.existsByUserId("uid-99")).thenReturn(false);

        mockMvc.perform(get("/api/user-service/secure/findUserId/uid-99")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void findUserById_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/user-service/secure/findUserId/uid-1")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- findEmailId ---

    @Test
    void findUserByEmail_exists_returnsTrue() throws Exception {
        when(userRepository.existsByUserEmail("target@example.com")).thenReturn(true);

        mockMvc.perform(get("/api/user-service/secure/findEmailId/target@example.com")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void findUserByEmail_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/user-service/secure/findEmailId/target@example.com")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- updateUser ---

    @Test
    void updateUser_success() throws Exception {
        User existing = buildUser(USER_EMAIL, UserRole.USER);
        when(userRepository.existsByUserEmail(USER_EMAIL)).thenReturn(true);
        when(userRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserModel userModel = new UserModel();
        userModel.setUserEmail(USER_EMAIL);
        userModel.setUserPassword("newpass1");
        userModel.setUserName("Updated Name");
        userModel.setUserAge(30);

        mockMvc.perform(post("/api/user-service/secure/updateUser")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userModel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("Updated Name"));
    }

    @Test
    void updateUser_unauthorized_returns403() throws Exception {
        UserModel userModel = new UserModel();
        userModel.setUserEmail(ADMIN_EMAIL);
        userModel.setUserPassword("newpass1");
        userModel.setUserName("Updated");
        userModel.setUserAge(30);

        mockMvc.perform(post("/api/user-service/secure/updateUser")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userModel)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        when(userRepository.existsByUserEmail(USER_EMAIL)).thenReturn(false);

        UserModel userModel = new UserModel();
        userModel.setUserEmail(USER_EMAIL);
        userModel.setUserPassword("newpass1");
        userModel.setUserName("Updated");
        userModel.setUserAge(30);

        mockMvc.perform(post("/api/user-service/secure/updateUser")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userModel)))
                .andExpect(status().isNotFound());
    }

    // --- requestAdminAccess ---

    @Test
    void requestAdminAccess_success() throws Exception {
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/api/user-service/secure/requestAdminAccess")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestRoleDto())))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Request for Admin access sent successfully"));
    }

    @Test
    void requestAdminAccess_unauthorized_returns403() throws Exception {
        mockMvc.perform(post("/api/user-service/secure/requestAdminAccess")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestRoleDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestAdminAccess_kafkaFailure_returns503() throws Exception {
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka down")));

        mockMvc.perform(post("/api/user-service/secure/requestAdminAccess")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestRoleDto())))
                .andExpect(status().isServiceUnavailable());
    }

    // --- requestDoctorAccess ---

    @Test
    void requestDoctorAccess_success() throws Exception {
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        RequestRoleDto dto = new RequestRoleDto();
        dto.setDoctorDto(new DoctorDto());

        mockMvc.perform(post("/api/user-service/secure/requestDoctorAccess")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Request for Doctor access sent successfully"));
    }

    @Test
    void requestDoctorAccess_unauthorized_returns403() throws Exception {
        mockMvc.perform(post("/api/user-service/secure/requestDoctorAccess")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestRoleDto())))
                .andExpect(status().isForbidden());
    }

    // --- requestPatientAccess ---

    @Test
    void requestPatientAccess_success() throws Exception {
        User user = buildUser(USER_EMAIL, UserRole.USER);
        when(userRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        RequestRoleDto dto = new RequestRoleDto();
        dto.setPatientDto(new PatientDto());

        mockMvc.perform(post("/api/user-service/secure/requestPatientAccess")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Request for Patient access sent successfully"));
    }

    @Test
    void requestPatientAccess_unauthorized_returns403() throws Exception {
        mockMvc.perform(post("/api/user-service/secure/requestPatientAccess")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestRoleDto())))
                .andExpect(status().isForbidden());
    }

    // --- changeRole ---

    @Test
    void changeRole_success() throws Exception {
        User user = buildUser("target@example.com", UserRole.USER);
        when(userRepository.findByUserEmail("target@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangeRequest changeRequest = ChangeRequest.builder()
                .email("target@example.com")
                .role(UserRole.DOCTOR)
                .build();

        mockMvc.perform(post("/api/user-service/secure/changeRole")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("target@example.com"));
    }

    @Test
    void changeRole_unauthorized_returns403() throws Exception {
        ChangeRequest changeRequest = ChangeRequest.builder()
                .email("target@example.com")
                .role(UserRole.DOCTOR)
                .build();

        mockMvc.perform(post("/api/user-service/secure/changeRole")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isForbidden());
    }

    // --- checkStatus ---

    @Test
    void checkStatus_success() throws Exception {
        RequestRoleDto dto = new RequestRoleDto();
        dto.setUserEmail(USER_EMAIL);
        dto.setRequestStatus(Status.PENDING);

        when(externalServiceClient.checkStatus(USER_EMAIL)).thenReturn(dto);

        mockMvc.perform(get("/api/user-service/secure/checkStatus")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value(USER_EMAIL));
    }

    @Test
    void checkStatus_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/user-service/secure/checkStatus")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isForbidden());
    }
}
