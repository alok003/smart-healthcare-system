package com.project.adminService.Service;

import com.project.adminService.Model.*;
import com.project.adminService.RESTCalls.DoctorClient;
import com.project.adminService.RESTCalls.PatientClient;
import com.project.adminService.RESTCalls.UserClient;
import com.project.adminService.Utility.LogUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ExternalServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceClient.class);

    private UserClient userClient;
    private DoctorClient doctorClient;
    private PatientClient patientClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "changeRoleFallback")
    @Retry(name = "userService")
    public String changeUserRole(ChangeRequest changeRequest, String email) {
        log.info("action=FEIGN_REQUEST service=user-service endpoint=/api/user-service/secure/changeRole identifier={} targetRole={} payload={}", changeRequest.getEmail(), changeRequest.getRole(), LogUtil.toJson(changeRequest));
        String result = userClient.changeRole(changeRequest, email, UserRole.ADMIN.name());
        log.info("action=FEIGN_RESPONSE service=user-service status=SUCCESS identifier={} targetRole={} payload={}", changeRequest.getEmail(), changeRequest.getRole(), result);
        return result;
    }

    private String changeRoleFallback(ChangeRequest changeRequest, String email, Exception e) {
        log.error("action=FEIGN_RESPONSE service=user-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", changeRequest.getEmail(), e.getMessage());
        throw new RuntimeException("User service unavailable, unable to change role for: " + changeRequest.getEmail());
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "saveDoctorFallback")
    @Retry(name = "doctorService")
    public String saveDoctorProfile(DoctorDto doctorDto, Integer maxCount, Double rate, String email) {
        log.info("action=FEIGN_REQUEST service=doctor-service endpoint=/api/doctor-service/secure/saveDoctor identifier={} payload={}", doctorDto.getEmail(), LogUtil.toJson(doctorDto));
        String result = doctorClient.saveDoctor(doctorDto, maxCount, rate, email, UserRole.ADMIN.name());
        log.info("action=FEIGN_RESPONSE service=doctor-service status=SUCCESS identifier={} payload={}", doctorDto.getEmail(), result);
        return result;
    }

    private String saveDoctorFallback(DoctorDto doctorDto, Integer maxCount, Double rate, String email, Exception e) {
        log.error("action=FEIGN_RESPONSE service=doctor-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", doctorDto.getEmail(), e.getMessage());
        throw new RuntimeException("Doctor service unavailable, unable to save doctor profile for: " + doctorDto.getEmail());
    }

    @CircuitBreaker(name = "patientService", fallbackMethod = "savePatientFallback")
    @Retry(name = "patientService")
    public String savePatientProfile(PatientDto patientDto, String email) {
        log.info("action=FEIGN_REQUEST service=patient-service endpoint=/api/patient-service/secure/savePatient identifier={} payload={}", patientDto.getEmail(), LogUtil.toJson(patientDto));
        String result = patientClient.savePatient(patientDto, email, UserRole.ADMIN.name());
        log.info("action=FEIGN_RESPONSE service=patient-service status=SUCCESS identifier={} payload={}", patientDto.getEmail(), result);
        return result;
    }

    private String savePatientFallback(PatientDto patientDto, String email, Exception e) {
        log.error("action=FEIGN_RESPONSE service=patient-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", patientDto.getEmail(), e.getMessage());
        throw new RuntimeException("Patient service unavailable, unable to save patient profile for: " + patientDto.getEmail());
    }
}
