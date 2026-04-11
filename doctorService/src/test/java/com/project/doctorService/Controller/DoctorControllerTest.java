package com.project.doctorService.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.doctorService.Entity.Doctor;
import com.project.doctorService.Model.*;
import com.project.doctorService.Repository.DoctorRepository;
import com.project.doctorService.Service.ExternalServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorRepository doctorRepository;

    @MockitoBean
    private ExternalServiceClient externalServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String DOCTOR_EMAIL = "doctor@example.com";
    private static final String DOCTOR_ROLE = "DOCTOR";
    private static final String USER_EMAIL = "user@example.com";
    private static final String USER_ROLE = "USER";

    private Doctor buildDoctor(String email) {
        Doctor d = new Doctor();
        d.setId("doc-1");
        d.setEmail(email);
        d.setGender(Gender.MALE);
        d.setLicenseNumber("LIC-001");
        d.setContactNumber("9999999999");
        Bookings bookings = new Bookings();
        bookings.setMaxCount(5);
        bookings.setRate(300.0);
        bookings.setBookingListMap(new HashMap<>());
        d.setBookings(bookings);
        return d;
    }

    // --- health ---

    @Test
    void health_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/doctor-service/open/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // --- saveDoctor ---

    @Test
    void saveDoctor_newDoctor_returnsCreated() throws Exception {
        when(doctorRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.empty());
        when(doctorRepository.save(any(Doctor.class))).thenReturn(buildDoctor(DOCTOR_EMAIL));

        DoctorDto dto = new DoctorDto();
        dto.setEmail(DOCTOR_EMAIL);
        dto.setGender(Gender.MALE);
        dto.setLicenseNumber("LIC-001");

        mockMvc.perform(post("/api/doctor-service/secure/saveDoctor")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .param("maxCount", "5")
                        .param("rate", "300.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(DOCTOR_EMAIL));
    }

    @Test
    void saveDoctor_existingDoctor_returnsExisting() throws Exception {
        when(doctorRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.of(buildDoctor(DOCTOR_EMAIL)));

        DoctorDto dto = new DoctorDto();
        dto.setEmail(DOCTOR_EMAIL);

        mockMvc.perform(post("/api/doctor-service/secure/saveDoctor")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .param("maxCount", "5")
                        .param("rate", "300.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(DOCTOR_EMAIL));
    }

    @Test
    void saveDoctor_unauthorized_returns403() throws Exception {
        mockMvc.perform(post("/api/doctor-service/secure/saveDoctor")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .param("maxCount", "5")
                        .param("rate", "300.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorDto())))
                .andExpect(status().isForbidden());
    }

    // --- getMyDetails ---

    @Test
    void getMyDetails_success() throws Exception {
        when(doctorRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.of(buildDoctor(DOCTOR_EMAIL)));

        mockMvc.perform(get("/api/doctor-service/secure/getMyDetails")
                        .header("X-User-Email", DOCTOR_EMAIL)
                        .header("X-User-Role", DOCTOR_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(DOCTOR_EMAIL));
    }

    @Test
    void getMyDetails_notFound_returns404() throws Exception {
        when(doctorRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/doctor-service/secure/getMyDetails")
                        .header("X-User-Email", DOCTOR_EMAIL)
                        .header("X-User-Role", DOCTOR_ROLE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyDetails_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/doctor-service/secure/getMyDetails")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- getMyAppointments ---

    @Test
    void getMyAppointments_success() throws Exception {
        when(externalServiceClient.getAppointmentsByDoctor(DOCTOR_EMAIL, DOCTOR_EMAIL, "ADMIN")).thenReturn(List.of());

        mockMvc.perform(get("/api/doctor-service/secure/getMyAppointments")
                        .header("X-User-Email", DOCTOR_EMAIL)
                        .header("X-User-Role", DOCTOR_ROLE))
                .andExpect(status().isOk());
    }

    @Test
    void getMyAppointments_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/doctor-service/secure/getMyAppointments")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- getAllDoctors ---

    @Test
    void getAllDoctors_success() throws Exception {
        when(doctorRepository.findAll()).thenReturn(List.of(buildDoctor(DOCTOR_EMAIL)));

        mockMvc.perform(get("/api/doctor-service/secure/getAllDoctors")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(DOCTOR_EMAIL));
    }

    @Test
    void getAllDoctors_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/doctor-service/secure/getAllDoctors")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- addLeave ---

    @Test
    void addLeave_success() throws Exception {
        Doctor doctor = buildDoctor(DOCTOR_EMAIL);
        Map<LocalDate, BookingList> map = new HashMap<>();
        map.put(LocalDate.now(), BookingList.builder().availibility(Availibility.AVAILABLE).build());
        doctor.getBookings().setBookingListMap(map);
        when(doctorRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        mockMvc.perform(post("/api/doctor-service/secure/addLeave")
                        .header("X-User-Email", DOCTOR_EMAIL)
                        .header("X-User-Role", DOCTOR_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(LocalDate.now()))))
                .andExpect(status().isOk());
    }

    @Test
    void addLeave_doctorNotFound_returns404() throws Exception {
        when(doctorRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/doctor-service/secure/addLeave")
                        .header("X-User-Email", DOCTOR_EMAIL)
                        .header("X-User-Role", DOCTOR_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(LocalDate.now()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addLeave_unauthorized_returns403() throws Exception {
        mockMvc.perform(post("/api/doctor-service/secure/addLeave")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(LocalDate.now()))))
                .andExpect(status().isForbidden());
    }

    // --- addDocAppointment ---

    @Test
    void addDocAppointment_success() throws Exception {
        Doctor doctor = buildDoctor(DOCTOR_EMAIL);
        Map<LocalDate, BookingList> map = new HashMap<>();
        map.put(LocalDate.now(), BookingList.builder().availibility(Availibility.AVAILABLE).build());
        doctor.getBookings().setBookingListMap(map);
        when(doctorRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        AppointmentDto dto = new AppointmentDto();
        dto.setId("appt-1");
        dto.setDoctorId(DOCTOR_EMAIL);
        dto.setDate(LocalDate.now());

        mockMvc.perform(post("/api/doctor-service/secure/addDocAppointment")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void addDocAppointment_nullBookingIdList_doesNotThrowNPE() throws Exception {
        Doctor doctor = buildDoctor(DOCTOR_EMAIL);
        Map<LocalDate, BookingList> map = new HashMap<>();
        BookingList slot = new BookingList();
        slot.setAvailibility(Availibility.AVAILABLE);
        slot.setBookingId(null);
        map.put(LocalDate.now(), slot);
        doctor.getBookings().setBookingListMap(map);
        when(doctorRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        AppointmentDto dto = new AppointmentDto();
        dto.setId("appt-1");
        dto.setDoctorId(DOCTOR_EMAIL);
        dto.setDate(LocalDate.now());

        mockMvc.perform(post("/api/doctor-service/secure/addDocAppointment")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void addDocAppointment_dateOutOfRange_returns400() throws Exception {
        Doctor doctor = buildDoctor(DOCTOR_EMAIL);
        when(doctorRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.of(doctor));

        AppointmentDto dto = new AppointmentDto();
        dto.setId("appt-1");
        dto.setDoctorId(DOCTOR_EMAIL);
        dto.setDate(LocalDate.now().plusDays(60));

        mockMvc.perform(post("/api/doctor-service/secure/addDocAppointment")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addDocAppointment_unauthorized_returns403() throws Exception {
        mockMvc.perform(post("/api/doctor-service/secure/addDocAppointment")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentDto())))
                .andExpect(status().isForbidden());
    }

    // --- completeAppointment ---

    @Test
    void completeAppointment_success() throws Exception {
        AppointmentDto appt = new AppointmentDto();
        appt.setId("appt-1");
        appt.setDoctorId(DOCTOR_EMAIL);
        appt.setPatientId("patient@example.com");
        when(externalServiceClient.getAppointmentById("appt-1", DOCTOR_EMAIL, "ADMIN")).thenReturn(appt);
        when(externalServiceClient.completeAppointment(any(VisitDetails.class), any(), any())).thenReturn(appt);

        VisitDetails visitDetails = new VisitDetails();
        visitDetails.setAppointmentId("appt-1");
        visitDetails.setPrescription("Take rest");
        visitDetails.setHealthCheck(buildHealthCheck());

        mockMvc.perform(post("/api/doctor-service/secure/completeAppointment")
                        .header("X-User-Email", DOCTOR_EMAIL)
                        .header("X-User-Role", DOCTOR_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visitDetails)))
                .andExpect(status().isOk());
    }

    @Test
    void completeAppointment_doctorMismatch_returns404() throws Exception {
        AppointmentDto appt = new AppointmentDto();
        appt.setId("appt-1");
        appt.setDoctorId("other@example.com");
        when(externalServiceClient.getAppointmentById("appt-1", DOCTOR_EMAIL, "ADMIN")).thenReturn(appt);

        VisitDetails visitDetails = new VisitDetails();
        visitDetails.setAppointmentId("appt-1");
        visitDetails.setPrescription("Take rest");
        visitDetails.setHealthCheck(buildHealthCheck());

        mockMvc.perform(post("/api/doctor-service/secure/completeAppointment")
                        .header("X-User-Email", DOCTOR_EMAIL)
                        .header("X-User-Role", DOCTOR_ROLE)
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

        mockMvc.perform(post("/api/doctor-service/secure/completeAppointment")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visitDetails)))
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
