package com.project.adminService.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Model.*;
import com.project.adminService.Repository.AdminRepository;
import com.project.adminService.Service.ExternalServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private ExternalServiceClient externalServiceClient;

    @MockitoBean
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_EMAIL = "user@example.com";
    private static final String USER_ROLE = "USER";

    private RequestRole buildRequest(String id, String email, UserRole role, Status status) {
        RequestRole r = new RequestRole();
        r.setId(id);
        r.setUserEmail(email);
        r.setUserRole(role);
        r.setRequestStatus(status);
        return r;
    }

    // --- health ---

    @Test
    void health_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/admin-service/open/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // --- getAllRequests ---

    @Test
    void getAllRequests_success() throws Exception {
        when(adminRepository.findByRequestStatus(Status.PENDING))
                .thenReturn(List.of(buildRequest("id-1", USER_EMAIL, UserRole.PATIENT, Status.PENDING)));

        mockMvc.perform(get("/api/admin-service/secure/getAllRequests")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value(USER_EMAIL));
    }

    @Test
    void getAllRequests_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/admin-service/secure/getAllRequests")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- declineRequest ---

    @Test
    void declineRequest_success() throws Exception {
        RequestRole r = buildRequest("id-1", USER_EMAIL, UserRole.PATIENT, Status.PENDING);
        when(adminRepository.findById("id-1")).thenReturn(Optional.of(r));
        when(adminRepository.save(any(RequestRole.class))).thenReturn(r);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(put("/api/admin-service/secure/declineRequest/id-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(content().string("id-1"));
    }

    @Test
    void declineRequest_notFound_returns404() throws Exception {
        when(adminRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin-service/secure/declineRequest/nonexistent")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNotFound());
    }

    @Test
    void declineRequest_alreadyApproved_returns400() throws Exception {
        when(adminRepository.findById("id-1")).thenReturn(Optional.of(
                buildRequest("id-1", USER_EMAIL, UserRole.PATIENT, Status.APPROVED)));

        mockMvc.perform(put("/api/admin-service/secure/declineRequest/id-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void declineRequest_alreadyDiscarded_returns400() throws Exception {
        when(adminRepository.findById("id-1")).thenReturn(Optional.of(
                buildRequest("id-1", USER_EMAIL, UserRole.PATIENT, Status.DISCARDED)));

        mockMvc.perform(put("/api/admin-service/secure/declineRequest/id-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void declineRequest_unauthorized_returns403() throws Exception {
        mockMvc.perform(put("/api/admin-service/secure/declineRequest/id-1")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- approveRequest ---

    @Test
    void approveRequest_adminRole_success() throws Exception {
        RequestRole r = buildRequest("id-1", USER_EMAIL, UserRole.ADMIN, Status.PENDING);
        when(adminRepository.findById("id-1")).thenReturn(Optional.of(r));
        when(adminRepository.save(any(RequestRole.class))).thenReturn(r);
        when(externalServiceClient.changeUserRole(any(ChangeRequest.class), eq(ADMIN_EMAIL))).thenReturn(USER_EMAIL);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(put("/api/admin-service/secure/approve/id-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(content().string("id-1"));
    }

    @Test
    void approveRequest_doctorRole_success() throws Exception {
        RequestRole r = buildRequest("id-1", USER_EMAIL, UserRole.DOCTOR, Status.PENDING);
        DoctorDto doctorDto = new DoctorDto();
        doctorDto.setEmail(USER_EMAIL);
        r.setDoctorDto(doctorDto);
        when(adminRepository.findById("id-1")).thenReturn(Optional.of(r));
        when(adminRepository.save(any(RequestRole.class))).thenReturn(r);
        when(externalServiceClient.changeUserRole(any(ChangeRequest.class), eq(ADMIN_EMAIL))).thenReturn(USER_EMAIL);
        when(externalServiceClient.saveDoctorProfile(any(DoctorDto.class), any(Integer.class), any(Double.class), eq(ADMIN_EMAIL))).thenReturn("doctor-id");
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(put("/api/admin-service/secure/approve/id-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .param("maxCount", "10")
                        .param("rate", "500.0"))
                .andExpect(status().isOk())
                .andExpect(content().string("id-1"));
    }

    @Test
    void approveRequest_patientRole_success() throws Exception {
        RequestRole r = buildRequest("id-1", USER_EMAIL, UserRole.PATIENT, Status.PENDING);
        PatientDto patientDto = new PatientDto();
        patientDto.setEmail(USER_EMAIL);
        patientDto.setName("Test User");
        patientDto.setDateOfBirth(LocalDate.of(2000, 1, 1));
        patientDto.setGender(Gender.MALE);
        r.setPatientDto(patientDto);
        when(adminRepository.findById("id-1")).thenReturn(Optional.of(r));
        when(adminRepository.save(any(RequestRole.class))).thenReturn(r);
        when(externalServiceClient.changeUserRole(any(ChangeRequest.class), eq(ADMIN_EMAIL))).thenReturn(USER_EMAIL);
        when(externalServiceClient.savePatientProfile(any(PatientDto.class), eq(ADMIN_EMAIL))).thenReturn("patient-id");
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(put("/api/admin-service/secure/approve/id-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(content().string("id-1"));
    }

    @Test
    void approveRequest_notFound_returns404() throws Exception {
        when(adminRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin-service/secure/approve/nonexistent")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveRequest_alreadyApproved_returns400() throws Exception {
        when(adminRepository.findById("id-1")).thenReturn(Optional.of(
                buildRequest("id-1", USER_EMAIL, UserRole.ADMIN, Status.APPROVED)));

        mockMvc.perform(put("/api/admin-service/secure/approve/id-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveRequest_discarded_returns400() throws Exception {
        when(adminRepository.findById("id-1")).thenReturn(Optional.of(
                buildRequest("id-1", USER_EMAIL, UserRole.ADMIN, Status.DISCARDED)));

        mockMvc.perform(put("/api/admin-service/secure/approve/id-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveRequest_unauthorized_returns403() throws Exception {
        mockMvc.perform(put("/api/admin-service/secure/approve/id-1")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveRequest_doctorServiceDown_rollsBackAndReturns503() throws Exception {
        RequestRole r = buildRequest("id-1", USER_EMAIL, UserRole.DOCTOR, Status.PENDING);
        DoctorDto doctorDto = new DoctorDto();
        doctorDto.setEmail(USER_EMAIL);
        r.setDoctorDto(doctorDto);
        when(adminRepository.findById("id-1")).thenReturn(Optional.of(r));
        when(externalServiceClient.changeUserRole(any(ChangeRequest.class), eq(ADMIN_EMAIL))).thenReturn(USER_EMAIL);
        when(externalServiceClient.saveDoctorProfile(any(DoctorDto.class), any(Integer.class), any(Double.class), eq(ADMIN_EMAIL)))
                .thenThrow(new RuntimeException("Doctor service down"));

        mockMvc.perform(put("/api/admin-service/secure/approve/id-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .param("maxCount", "10")
                        .param("rate", "500.0"))
                .andExpect(status().isServiceUnavailable());
    }

    // --- approvePatients ---

    @Test
    void approvePatients_success() throws Exception {
        RequestRole r = buildRequest("id-1", USER_EMAIL, UserRole.PATIENT, Status.PENDING);
        PatientDto patientDto = new PatientDto();
        patientDto.setEmail(USER_EMAIL);
        r.setPatientDto(patientDto);
        when(adminRepository.findByUserRoleAndRequestStatus(UserRole.PATIENT, Status.PENDING)).thenReturn(List.of(r));
        when(adminRepository.save(any(RequestRole.class))).thenReturn(r);
        when(externalServiceClient.changeUserRole(any(ChangeRequest.class), anyString())).thenReturn(USER_EMAIL);
        when(externalServiceClient.savePatientProfile(any(PatientDto.class), anyString())).thenReturn("patient-id");

        mockMvc.perform(post("/api/admin-service/secure/approve/patients")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNoContent());
    }

    @Test
    void approvePatients_unauthorized_returns403() throws Exception {
        mockMvc.perform(post("/api/admin-service/secure/approve/patients")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- checkStatus ---

    @Test
    void checkStatus_success() throws Exception {
        when(adminRepository.findByUserEmail(USER_EMAIL))
                .thenReturn(Optional.of(buildRequest("id-1", USER_EMAIL, UserRole.PATIENT, Status.PENDING)));

        mockMvc.perform(get("/api/admin-service/secure/checkStatus")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .param("userEmail", USER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value(USER_EMAIL))
                .andExpect(jsonPath("$.requestStatus").value("PENDING"));
    }

    @Test
    void checkStatus_notFound_returns404() throws Exception {
        when(adminRepository.findByUserEmail("notfound@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin-service/secure/checkStatus")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .param("userEmail", "notfound@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkStatus_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/admin-service/secure/checkStatus")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .param("userEmail", USER_EMAIL))
                .andExpect(status().isForbidden());
    }
}
