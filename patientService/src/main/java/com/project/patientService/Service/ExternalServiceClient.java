package com.project.patientService.Service;

import com.project.patientService.Model.AppointmentDto;
import com.project.patientService.Model.DoctorDto;
import com.project.patientService.RESTCalls.AppointmentClient;
import com.project.patientService.RESTCalls.DoctorClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ExternalServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceClient.class);

    private AppointmentClient appointmentClient;
    private DoctorClient doctorClient;

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "bookAppointmentFallback")
    @Retry(name = "appointmentService")
    public AppointmentDto bookAppointment(AppointmentDto appointmentDto, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/bookAppointment identifier={}", email);
        AppointmentDto result = appointmentClient.bookAppointment(appointmentDto, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={} appointmentId={}", email, result.getId());
        return result;
    }

    private AppointmentDto bookAppointmentFallback(AppointmentDto appointmentDto, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", email, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to book appointment: " + e.getMessage());
    }

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "deleteAppointmentFallback")
    @Retry(name = "appointmentService")
    public void deleteAppointment(String id, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/deleteAppointment identifier={}", id);
        appointmentClient.deleteAppointment(id, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={}", id);
    }

    private void deleteAppointmentFallback(String id, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", id, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to delete appointment: " + e.getMessage());
    }

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "markCancelledFallback")
    @Retry(name = "appointmentService")
    public void markCancelled(String id, String cancelledBy, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/markCancelled identifier={} cancelledBy={}", id, cancelledBy);
        appointmentClient.markCancelled(id, cancelledBy, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={}", id);
    }

    private void markCancelledFallback(String id, String cancelledBy, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", id, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to mark cancelled: " + e.getMessage());
    }

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "restoreAppointmentFallback")
    @Retry(name = "appointmentService")
    public void restoreAppointment(String id, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/restoreAppointment identifier={}", id);
        appointmentClient.restoreAppointment(id, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={}", id);
    }

    private void restoreAppointmentFallback(String id, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", id, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to restore appointment: " + e.getMessage());
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "removeFromScheduleFallback")
    @Retry(name = "doctorService")
    public void removeAppointmentFromSchedule(String appointmentId, String date, String email, String role) {
        log.info("action=FEIGN_REQUEST service=doctor-service endpoint=/removeAppointmentFromSchedule identifier={}", appointmentId);
        doctorClient.removeAppointmentFromSchedule(appointmentId, date, email, role);
        log.info("action=FEIGN_RESPONSE service=doctor-service status=SUCCESS identifier={}", appointmentId);
    }

    private void removeFromScheduleFallback(String appointmentId, String date, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=doctor-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", appointmentId, e.getMessage());
        throw new RuntimeException("Doctor service unavailable, unable to remove from schedule: " + e.getMessage());
    }

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "getAppointmentsByPatientFallback")
    @Retry(name = "appointmentService")
    public List<AppointmentDto> getAppointmentsByPatient(String userId, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/getAppointmentsByUserId identifier={}", userId);
        List<AppointmentDto> result = appointmentClient.getAllAppointmentsbyPatient(userId, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={} count={}", userId, result.size());
        return result;
    }

    private List<AppointmentDto> getAppointmentsByPatientFallback(String userId, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", userId, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to fetch appointments: " + e.getMessage());
    }

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "getAppointmentByIdFallback")
    @Retry(name = "appointmentService")
    public AppointmentDto getAppointmentById(String id, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/getAppointment identifier={}", id);
        AppointmentDto result = appointmentClient.getAppointmentById(id, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={}", id);
        return result;
    }

    private AppointmentDto getAppointmentByIdFallback(String id, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", id, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to fetch appointment: " + e.getMessage());
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "getDoctorByEmailFallback")
    @Retry(name = "doctorService")
    public DoctorDto getDoctorByEmail(String doctorEmail, String email, String role) {
        log.info("action=FEIGN_REQUEST service=doctor-service endpoint=/getDoctorByEmail identifier={}", doctorEmail);
        DoctorDto result = doctorClient.getDoctorByEmail(doctorEmail, email, role);
        log.info("action=FEIGN_RESPONSE service=doctor-service status=SUCCESS identifier={}", doctorEmail);
        return result;
    }

    private DoctorDto getDoctorByEmailFallback(String doctorEmail, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=doctor-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", doctorEmail, e.getMessage());
        throw new RuntimeException("Doctor service unavailable, unable to fetch doctor: " + e.getMessage());
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "getAllDoctorsFallback")
    @Retry(name = "doctorService")
    public List<DoctorDto> getAllDoctors(String email, String role) {
        log.info("action=FEIGN_REQUEST service=doctor-service endpoint=/getAllDoctors requestedBy={}", email);
        List<DoctorDto> result = doctorClient.getAllDoctorList(email, role);
        log.info("action=FEIGN_RESPONSE service=doctor-service status=SUCCESS requestedBy={} count={}", email, result.size());
        return result;
    }

    private List<DoctorDto> getAllDoctorsFallback(String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=doctor-service status=FAILED requestedBy={} reason=SERVICE_UNAVAILABLE error={}", email, e.getMessage());
        throw new RuntimeException("Doctor service unavailable, unable to fetch doctors: " + e.getMessage());
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "addDocAppointmentFallback")
    @Retry(name = "doctorService")
    public Boolean addDocAppointment(AppointmentDto appointmentDto, String email, String role) {
        log.info("action=FEIGN_REQUEST service=doctor-service endpoint=/addDocAppointment identifier={}", appointmentDto.getId());
        Boolean result = doctorClient.addDocAppointment(appointmentDto, email, role);
        log.info("action=FEIGN_RESPONSE service=doctor-service status=SUCCESS identifier={}", appointmentDto.getId());
        return result;
    }

    private Boolean addDocAppointmentFallback(AppointmentDto appointmentDto, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=doctor-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", appointmentDto.getId(), e.getMessage());
        throw new RuntimeException("Doctor service unavailable, unable to update doctor schedule: " + e.getMessage());
    }
}
