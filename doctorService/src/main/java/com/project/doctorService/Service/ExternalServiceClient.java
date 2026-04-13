package com.project.doctorService.Service;

import com.project.doctorService.Model.AppointmentDto;
import com.project.doctorService.Model.VisitDetails;
import com.project.doctorService.RESTCalls.AppointmentClient;
import com.project.doctorService.RESTCalls.PatientClient;
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
    private PatientClient patientClient;

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "markCancelledFallback")
    @Retry(name = "appointmentService")
    public void markCancelled(String appointmentId, String cancelledBy, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/markCancelled identifier={} cancelledBy={}", appointmentId, cancelledBy);
        appointmentClient.markCancelled(appointmentId, cancelledBy, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={}", appointmentId);
    }

    private void markCancelledFallback(String appointmentId, String cancelledBy, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", appointmentId, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to mark cancelled: " + e.getMessage());
    }

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "restoreAppointmentFallback")
    @Retry(name = "appointmentService")
    public void restoreAppointment(String appointmentId, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/restoreAppointment identifier={}", appointmentId);
        appointmentClient.restoreAppointment(appointmentId, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={}", appointmentId);
    }

    private void restoreAppointmentFallback(String appointmentId, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", appointmentId, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to restore appointment: " + e.getMessage());
    }

    @CircuitBreaker(name = "patientService", fallbackMethod = "removeAppointmentFromPatientFallback")
    @Retry(name = "patientService")
    public void removeAppointmentFromPatient(String appointmentId, String email, String role) {
        log.info("action=FEIGN_REQUEST service=patient-service endpoint=/removeAppointment identifier={}", appointmentId);
        patientClient.removeAppointmentFromPatient(appointmentId, email, role);
        log.info("action=FEIGN_RESPONSE service=patient-service status=SUCCESS identifier={}", appointmentId);
    }

    private void removeAppointmentFromPatientFallback(String appointmentId, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=patient-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", appointmentId, e.getMessage());
        throw new RuntimeException("Patient service unavailable, unable to remove appointment: " + e.getMessage());
    }

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "completeAppointmentFallback")
    @Retry(name = "appointmentService")
    public AppointmentDto completeAppointment(VisitDetails visitDetails, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/completeAppointment identifier={}", visitDetails.getAppointmentId());
        AppointmentDto result = appointmentClient.completeAppointment(visitDetails, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={}", visitDetails.getAppointmentId());
        return result;
    }

    private AppointmentDto completeAppointmentFallback(VisitDetails visitDetails, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", visitDetails.getAppointmentId(), e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to complete appointment: " + e.getMessage());
    }

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "getAppointmentByIdFallback")
    @Retry(name = "appointmentService")
    public AppointmentDto getAppointmentById(String appointmentId, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/getAppointment identifier={}", appointmentId);
        AppointmentDto result = appointmentClient.getAppointmentById(appointmentId, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={}", appointmentId);
        return result;
    }

    private AppointmentDto getAppointmentByIdFallback(String appointmentId, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", appointmentId, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to fetch appointment: " + e.getMessage());
    }

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "getAppointmentsByDoctorFallback")
    @Retry(name = "appointmentService")
    public List<AppointmentDto> getAppointmentsByDoctor(String doctorId, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/getAppointmentsByDoctorId identifier={}", doctorId);
        List<AppointmentDto> result = appointmentClient.getAppointmentsByDoctorId(doctorId, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={} count={}", doctorId, result.size());
        return result;
    }

    private List<AppointmentDto> getAppointmentsByDoctorFallback(String doctorId, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", doctorId, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to fetch appointments: " + e.getMessage());
    }
}
