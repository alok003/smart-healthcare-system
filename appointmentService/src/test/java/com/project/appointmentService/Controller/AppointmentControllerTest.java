package com.project.appointmentService.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.appointmentService.Entity.Appointment;
import com.project.appointmentService.Model.AppointmentDto;
import com.project.appointmentService.Model.HealthCheck;
import com.project.appointmentService.Model.Status;
import com.project.appointmentService.Model.VisitDetails;
import com.project.appointmentService.Repository.AppointmentRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_EMAIL = "user@example.com";
    private static final String USER_ROLE = "USER";

    private Appointment buildAppointment(String id, Status status) {
        Appointment a = new Appointment();
        a.setId(id);
        a.setPatientId("patient@example.com");
        a.setDoctorId("doctor@example.com");
        a.setStatus(status);
        a.setDate(LocalDate.now());
        a.setSubject("Checkup");
        return a;
    }

    // --- health ---

    @Test
    void health_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/appointment-service/open/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // --- bookAppointment ---

    @Test
    void bookAppointment_success() throws Exception {
        Appointment saved = buildAppointment("appt-1", Status.UPCOMING);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        AppointmentDto dto = new AppointmentDto();
        dto.setPatientId("patient@example.com");
        dto.setDoctorId("doctor@example.com");
        dto.setStatus(Status.UPCOMING);
        dto.setDate(LocalDate.now());
        dto.setSubject("Checkup");

        mockMvc.perform(post("/api/appointment-service/secure/bookAppointment")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("appt-1"))
                .andExpect(jsonPath("$.status").value("UPCOMING"));
    }

    @Test
    void bookAppointment_unauthorized_returns403() throws Exception {
        AppointmentDto dto = new AppointmentDto();
        dto.setPatientId("patient@example.com");
        dto.setDoctorId("doctor@example.com");
        dto.setDate(LocalDate.now());

        mockMvc.perform(post("/api/appointment-service/secure/bookAppointment")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // --- markCancelled ---

    @Test
    void markCancelled_success() throws Exception {
        Appointment existing = buildAppointment("appt-1", Status.UPCOMING);
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existing);

        mockMvc.perform(put("/api/appointment-service/secure/markCancelled/appt-1")
                        .param("cancelledBy", "patient@example.com")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNoContent());
    }

    @Test
    void markCancelled_notFound_returns404() throws Exception {
        when(appointmentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/appointment-service/secure/markCancelled/nonexistent")
                        .param("cancelledBy", "patient@example.com")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNotFound());
    }

    @Test
    void markCancelled_unauthorized_returns403() throws Exception {
        mockMvc.perform(put("/api/appointment-service/secure/markCancelled/appt-1")
                        .param("cancelledBy", "patient@example.com")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- restoreAppointment ---

    @Test
    void restoreAppointment_success() throws Exception {
        Appointment existing = buildAppointment("appt-1", Status.CANCELLED);
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existing);

        mockMvc.perform(put("/api/appointment-service/secure/restoreAppointment/appt-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNoContent());
    }

    @Test
    void restoreAppointment_unauthorized_returns403() throws Exception {
        mockMvc.perform(put("/api/appointment-service/secure/restoreAppointment/appt-1")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- deleteAppointment ---

    @Test
    void deleteAppointment_success() throws Exception {
        when(appointmentRepository.existsById("appt-1")).thenReturn(true);

        mockMvc.perform(delete("/api/appointment-service/secure/deleteAppointment/appt-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAppointment_notFound_returns404() throws Exception {
        when(appointmentRepository.existsById("nonexistent")).thenReturn(false);

        mockMvc.perform(delete("/api/appointment-service/secure/deleteAppointment/nonexistent")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAppointment_unauthorized_returns403() throws Exception {
        mockMvc.perform(delete("/api/appointment-service/secure/deleteAppointment/appt-1")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- completeAppointment ---

    @Test
    void completeAppointment_success() throws Exception {
        Appointment existing = buildAppointment("appt-1", Status.UPCOMING);
        Appointment completed = buildAppointment("appt-1", Status.VISITED);
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(completed);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        VisitDetails visitDetails = new VisitDetails();
        visitDetails.setAppointmentId("appt-1");
        visitDetails.setPrescription("Take rest");
        visitDetails.setHealthCheck(buildHealthCheck());

        mockMvc.perform(post("/api/appointment-service/secure/completeAppointment")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visitDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VISITED"));
    }

    @Test
    void completeAppointment_notFound_returns404() throws Exception {
        when(appointmentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        VisitDetails visitDetails = new VisitDetails();
        visitDetails.setAppointmentId("nonexistent");
        visitDetails.setPrescription("Take rest");
        visitDetails.setHealthCheck(buildHealthCheck());

        mockMvc.perform(post("/api/appointment-service/secure/completeAppointment")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visitDetails)))
                .andExpect(status().isNotFound());
    }

    @Test
    void completeAppointment_unauthorized_returns403() throws Exception {
        VisitDetails visitDetails = new VisitDetails();
        visitDetails.setAppointmentId("appt-1");
        visitDetails.setPrescription("Take rest");
        visitDetails.setHealthCheck(buildHealthCheck());

        mockMvc.perform(post("/api/appointment-service/secure/completeAppointment")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visitDetails)))
                .andExpect(status().isForbidden());
    }

    // --- getAppointment ---

    @Test
    void getAppointment_success() throws Exception {
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(buildAppointment("appt-1", Status.UPCOMING)));

        mockMvc.perform(get("/api/appointment-service/secure/getAppointment/appt-1")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("appt-1"));
    }

    @Test
    void getAppointment_notFound_returns404() throws Exception {
        when(appointmentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/appointment-service/secure/getAppointment/nonexistent")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAppointment_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/appointment-service/secure/getAppointment/appt-1")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- getAllAppointments ---

    @Test
    void getAllAppointments_success() throws Exception {
        when(appointmentRepository.findAll()).thenReturn(List.of(buildAppointment("appt-1", Status.UPCOMING)));

        mockMvc.perform(get("/api/appointment-service/secure/getAllAppointments")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("appt-1"));
    }

    @Test
    void getAllAppointments_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/appointment-service/secure/getAllAppointments")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- getAppointmentsByUserId ---

    @Test
    void getAppointmentsByUserId_success() throws Exception {
        when(appointmentRepository.findByPatientId("patient@example.com"))
                .thenReturn(List.of(buildAppointment("appt-1", Status.UPCOMING)));

        mockMvc.perform(get("/api/appointment-service/secure/getAppointmentsByUserId/patient@example.com")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value("patient@example.com"));
    }

    @Test
    void getAppointmentsByUserId_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/appointment-service/secure/getAppointmentsByUserId/patient@example.com")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- getAppointmentsByDoctorId ---

    @Test
    void getAppointmentsByDoctorId_success() throws Exception {
        when(appointmentRepository.findByDoctorId("doctor@example.com"))
                .thenReturn(List.of(buildAppointment("appt-1", Status.UPCOMING)));

        mockMvc.perform(get("/api/appointment-service/secure/getAppointmentsByDoctorId/doctor@example.com")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctorId").value("doctor@example.com"));
    }

    @Test
    void getAppointmentsByDoctorId_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/appointment-service/secure/getAppointmentsByDoctorId/doctor@example.com")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    private HealthCheck buildHealthCheck() {
        HealthCheck hc = new HealthCheck();
        hc.setHeight(170);
        hc.setWeight(70.0f);
        hc.setBpSys(120);
        hc.setBpDia(80);
        hc.setOxyLvl(98);
        hc.setBloodSugar(90);
        hc.setHeartRate(72);
        hc.setBodyTemperature(36.6f);
        hc.setRespiratoryRate(16);
        return hc;
    }
}
