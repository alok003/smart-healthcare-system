package com.project.userService.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userService.Entity.User;
import com.project.userService.Model.LoginRequest;
import com.project.userService.Model.UserModel;
import com.project.userService.Model.UserRole;
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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

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

    private User buildUser(String email, String rawPassword) {
        User user = new User();
        user.setUserEmail(email);
        user.setUserPassword(passwordEncoder.encode(rawPassword));
        user.setUserName("Test User");
        user.setUserAge(25);
        user.setUserRole(UserRole.USER);
        return user;
    }

    @Test
    void health_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/user-service/open/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void login_success() throws Exception {
        User user = buildUser("test@example.com", "password123");
        when(userRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setUserEmail("test@example.com");
        request.setUserPassword("password123");

        mockMvc.perform(post("/api/user-service/open/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.expiration").exists());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        User user = buildUser("test@example.com", "password123");
        when(userRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setUserEmail("test@example.com");
        request.setUserPassword("wrongpassword");

        mockMvc.perform(post("/api/user-service/open/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_userNotFound_returns401() throws Exception {
        when(userRepository.findByUserEmail("missing@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUserEmail("missing@example.com");
        request.setUserPassword("password123");

        mockMvc.perform(post("/api/user-service/open/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_invalidEmail_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUserEmail("not-an-email");
        request.setUserPassword("password123");

        mockMvc.perform(post("/api/user-service/open/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUserEmail("test@example.com");
        request.setUserPassword("");

        mockMvc.perform(post("/api/user-service/open/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_passwordTooShort_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUserEmail("test@example.com");
        request.setUserPassword("short");

        mockMvc.perform(post("/api/user-service/open/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addNewUser_success() throws Exception {
        when(userRepository.existsByUserEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(kafkaTemplate.send(eq("welcome-notification"), any(Map.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        UserModel userModel = new UserModel();
        userModel.setUserEmail("new@example.com");
        userModel.setUserPassword("password123");
        userModel.setUserName("John");
        userModel.setUserAge(25);

        mockMvc.perform(post("/api/user-service/open/newUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userModel)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userEmail").value("new@example.com"))
                .andExpect(jsonPath("$.userPassword").doesNotExist());
    }

    @Test
    void addNewUser_alreadyExists_returns409() throws Exception {
        when(userRepository.existsByUserEmail("existing@example.com")).thenReturn(true);

        UserModel userModel = new UserModel();
        userModel.setUserEmail("existing@example.com");
        userModel.setUserPassword("password123");
        userModel.setUserName("John");
        userModel.setUserAge(25);

        mockMvc.perform(post("/api/user-service/open/newUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userModel)))
                .andExpect(status().isConflict());
    }

    @Test
    void addNewUser_kafkaFailure_rollsBackAndReturns503() throws Exception {
        User saved = buildUser("new@example.com", "password123");
        when(userRepository.existsByUserEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(kafkaTemplate.send(eq("welcome-notification"), any(Map.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka down")));

        UserModel userModel = new UserModel();
        userModel.setUserEmail("new@example.com");
        userModel.setUserPassword("password123");
        userModel.setUserName("John");
        userModel.setUserAge(25);

        mockMvc.perform(post("/api/user-service/open/newUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userModel)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void addNewUser_invalidAge_returns400() throws Exception {
        UserModel userModel = new UserModel();
        userModel.setUserEmail("new@example.com");
        userModel.setUserPassword("password123");
        userModel.setUserName("John");
        userModel.setUserAge(5);

        mockMvc.perform(post("/api/user-service/open/newUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userModel)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addNewUser_missingName_returns400() throws Exception {
        UserModel userModel = new UserModel();
        userModel.setUserEmail("new@example.com");
        userModel.setUserPassword("password123");
        userModel.setUserAge(25);

        mockMvc.perform(post("/api/user-service/open/newUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userModel)))
                .andExpect(status().isBadRequest());
    }
}
