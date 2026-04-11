package com.project.doctorService.Service;

import com.project.doctorService.Model.AppointmentDto;
import com.project.doctorService.Model.VisitDetails;
import com.project.doctorService.RESTCalls.AppointmentClient;
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

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "cancelAppointmentFallback")
    @Retry(name = "appointmentService")
    public AppointmentDto cancelAppointment(String appointmentId, String email, String role) {
        log.info("action=FEIGN_REQUEST service=appointment-service endpoint=/cancelAppointment identifier={}", appointmentId);
        AppointmentDto result = appointmentClient.cancelAppointmentAppointmentClient(appointmentId, email, role);
        log.info("action=FEIGN_RESPONSE service=appointment-service status=SUCCESS identifier={}", appointmentId);
        return result;
    }

    private AppointmentDto cancelAppointmentFallback(String appointmentId, String email, String role, Exception e) {
        log.error("action=FEIGN_RESPONSE service=appointment-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", appointmentId, e.getMessage());
        throw new RuntimeException("Appointment service unavailable, unable to cancel appointment: " + e.getMessage());
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
