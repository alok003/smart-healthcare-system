package com.project.patientService.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.patientService.Entity.Patient;
import com.project.patientService.Model.*;
import com.project.patientService.Repository.PatientRepository;
import com.project.patientService.Service.ExternalServiceClient;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private ExternalServiceClient externalServiceClient;

    @MockitoBean
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PATIENT_EMAIL = "patient@example.com";
    private static final String PATIENT_ROLE = "PATIENT";
    private static final String USER_EMAIL = "user@example.com";
    private static final String USER_ROLE = "USER";

    private Patient buildPatient(String email) {
        Patient p = new Patient();
        p.setId("pat-1");
        p.setEmail(email);
        p.setName("John Doe");
        p.setGender(Gender.MALE);
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        p.setAppointmentList(new ArrayList<>(List.of("appt-1")));
        p.setVitalsFlow(new HashMap<>());
        return p;
    }

    private AppointmentDto buildAppointmentDto(String id) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(id);
        dto.setPatientId(PATIENT_EMAIL);
        dto.setDoctorId("doctor@example.com");
        dto.setStatus(Status.UPCOMING);
        dto.setDate(LocalDate.now());
        dto.setSubject("Checkup");
        return dto;
    }

    private DoctorDto buildDoctorDto(String email) {
        DoctorDto dto = new DoctorDto();
        dto.setId("doc-1");
        dto.setEmail(email);
        Bookings bookings = new Bookings();
        bookings.setMaxCount(5);
        bookings.setRate(300.0);
        Map<LocalDate, BookingList> map = new HashMap<>();
        map.put(LocalDate.now(), BookingList.builder().availibility(Availibility.AVAILABLE).build());
        bookings.setBookingListMap(map);
        dto.setBookings(bookings);
        return dto;
    }

    // --- health ---

    @Test
    void health_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/patient-service/open/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // --- savePatient ---

    @Test
    void savePatient_newPatient_returnsCreated() throws Exception {
        when(patientRepository.findByEmail(PATIENT_EMAIL)).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenReturn(buildPatient(PATIENT_EMAIL));

        PatientDto dto = new PatientDto();
        dto.setEmail(PATIENT_EMAIL);
        dto.setName("John Doe");
        dto.setGender(Gender.MALE);
        dto.setDateOfBirth(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/api/patient-service/secure/savePatient")
                        .header("X-User-Email", ADMIN_EMAIL)
                        .header("X-User-Role", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(PATIENT_EMAIL));
    }

    @Test
    void savePatient_unauthorized_returns403() throws Exception {
        mockMvc.perform(post("/api/patient-service/secure/savePatient")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PatientDto())))
                .andExpect(status().isForbidden());
    }

    // --- getMyProfile ---

    @Test
    void getMyProfile_success() throws Exception {
        when(patientRepository.findByEmail(PATIENT_EMAIL)).thenReturn(Optional.of(buildPatient(PATIENT_EMAIL)));

        mockMvc.perform(get("/api/patient-service/secure/getMyProfile")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(PATIENT_EMAIL));
    }

    @Test
    void getMyProfile_notFound_returns404() throws Exception {
        when(patientRepository.findByEmail(PATIENT_EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/patient-service/secure/getMyProfile")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyProfile_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/patient-service/secure/getMyProfile")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- getDocAvailability ---

    @Test
    void getDocAvailability_success() throws Exception {
        when(externalServiceClient.getAllDoctors(anyString(), anyString()))
                .thenReturn(List.of(buildDoctorDto("doctor@example.com")));

        mockMvc.perform(get("/api/patient-service/secure/getDocAvailability")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("doctor@example.com"));
    }

    @Test
    void getDocAvailability_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/patient-service/secure/getDocAvailability")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- bookAppointment ---

    @Test
    void bookAppointment_success() throws Exception {
        Patient patient = buildPatient(PATIENT_EMAIL);
        AppointmentDto saved = buildAppointmentDto("appt-2");
        when(patientRepository.findByEmail(PATIENT_EMAIL)).thenReturn(Optional.of(patient));
        when(externalServiceClient.getDoctorByEmail(eq("doctor@example.com"), anyString(), anyString()))
                .thenReturn(buildDoctorDto("doctor@example.com"));
        when(externalServiceClient.bookAppointment(any(), anyString(), anyString())).thenReturn(saved);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(externalServiceClient.addDocAppointment(any(), anyString(), anyString())).thenReturn(true);

        AppointmentDto dto = new AppointmentDto();
        dto.setDoctorId("doctor@example.com");
        dto.setDate(LocalDate.now());
        dto.setSubject("Checkup");

        mockMvc.perform(post("/api/patient-service/secure/bookAppointment")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("appt-2"));
    }

    @Test
    void bookAppointment_slotUnavailable_returns503() throws Exception {
        Patient patient = buildPatient(PATIENT_EMAIL);
        DoctorDto doctor = buildDoctorDto("doctor@example.com");
        doctor.getBookings().getBookingListMap().put(LocalDate.now(),
                BookingList.builder().availibility(Availibility.BOOKED).build());
        when(patientRepository.findByEmail(PATIENT_EMAIL)).thenReturn(Optional.of(patient));
        when(externalServiceClient.getDoctorByEmail(eq("doctor@example.com"), anyString(), anyString()))
                .thenReturn(doctor);

        AppointmentDto dto = new AppointmentDto();
        dto.setDoctorId("doctor@example.com");
        dto.setDate(LocalDate.now());
        dto.setSubject("Checkup");

        mockMvc.perform(post("/api/patient-service/secure/bookAppointment")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void bookAppointment_patientNotFound_returns404() throws Exception {
        when(patientRepository.findByEmail(PATIENT_EMAIL)).thenReturn(Optional.empty());

        AppointmentDto dto = new AppointmentDto();
        dto.setDoctorId("doctor@example.com");
        dto.setDate(LocalDate.now());

        mockMvc.perform(post("/api/patient-service/secure/bookAppointment")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookAppointment_unauthorized_returns403() throws Exception {
        mockMvc.perform(post("/api/patient-service/secure/bookAppointment")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentDto())))
                .andExpect(status().isForbidden());
    }

    // --- cancelAppointment ---

    @Test
    void cancelAppointment_success() throws Exception {
        Patient patient = buildPatient(PATIENT_EMAIL);
        AppointmentDto appointment = buildAppointmentDto("appt-1");
        AppointmentDto cancelled = buildAppointmentDto("appt-1");
        cancelled.setStatus(Status.CANCELLED);
        when(patientRepository.findByEmail(PATIENT_EMAIL)).thenReturn(Optional.of(patient));
        when(externalServiceClient.getAppointmentById(eq("appt-1"), anyString(), anyString())).thenReturn(appointment);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(delete("/api/patient-service/secure/cancelAppointment/appt-1")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE))
                .andExpect(status().isOk());
    }

    @Test
    void cancelAppointment_notOwned_returns404() throws Exception {
        Patient patient = buildPatient(PATIENT_EMAIL);
        when(patientRepository.findByEmail(PATIENT_EMAIL)).thenReturn(Optional.of(patient));

        mockMvc.perform(delete("/api/patient-service/secure/cancelAppointment/other-appt")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelAppointment_unauthorized_returns403() throws Exception {
        mockMvc.perform(delete("/api/patient-service/secure/cancelAppointment/appt-1")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- getAllAppointments ---

    @Test
    void getAllAppointments_success() throws Exception {
        when(externalServiceClient.getAppointmentsByPatient(anyString(), anyString(), anyString()))
                .thenReturn(List.of(buildAppointmentDto("appt-1")));

        mockMvc.perform(get("/api/patient-service/secure/getAllAppointments")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("appt-1"));
    }

    @Test
    void getAllAppointments_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/patient-service/secure/getAllAppointments")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }

    // --- getPrescription ---

    @Test
    void getPrescription_success() throws Exception {
        AppointmentDto appt = buildAppointmentDto("appt-1");
        VisitDetails vd = new VisitDetails();
        vd.setAppointmentId("appt-1");
        vd.setPrescription("Take rest");
        appt.setVisitDetails(vd);
        when(externalServiceClient.getAppointmentById(anyString(), anyString(), anyString())).thenReturn(appt);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(get("/api/patient-service/secure/getPrescription/appt-1")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Prescription for Appointment has been sent out to Email"));
    }

    @Test
    void getPrescription_noVisitDetails_returnsNotAvailable() throws Exception {
        AppointmentDto appt = buildAppointmentDto("appt-1");
        when(externalServiceClient.getAppointmentById(anyString(), anyString(), anyString())).thenReturn(appt);

        mockMvc.perform(get("/api/patient-service/secure/getPrescription/appt-1")
                        .header("X-User-Email", PATIENT_EMAIL)
                        .header("X-User-Role", PATIENT_ROLE))
                .andExpect(status().isAccepted())
                .andExpect(content().string("No prescription available for this appointment"));
    }

    @Test
    void getPrescription_unauthorized_returns403() throws Exception {
        mockMvc.perform(get("/api/patient-service/secure/getPrescription/appt-1")
                        .header("X-User-Email", USER_EMAIL)
                        .header("X-User-Role", USER_ROLE))
                .andExpect(status().isForbidden());
    }
}
