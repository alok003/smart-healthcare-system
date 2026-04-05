package com.project.adminService.Service;

import com.project.adminService.Model.*;
import com.project.adminService.RESTCalls.DoctorClient;
import com.project.adminService.RESTCalls.PatientClient;
import com.project.adminService.RESTCalls.UserClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ExternalServiceClient {

    private UserClient userClient;
    private DoctorClient doctorClient;
    private PatientClient patientClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "changeRoleFallback")
    @Retry(name = "userService")
    public String changeUserRole(ChangeRequest changeRequest, String email) {
        return userClient.changeRole(changeRequest, email, UserRole.ADMIN.name());
    }

    private String changeRoleFallback(ChangeRequest changeRequest, String email, Exception e) {
        System.out.println("changeUserRole fallback triggered for: " + changeRequest.getEmail() + ". Error: " + e.getMessage());
        throw new RuntimeException("User service unavailable, unable to change role for: " + changeRequest.getEmail());
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "saveDoctorFallback")
    @Retry(name = "doctorService")
    public String saveDoctorProfile(DoctorDto doctorDto, Integer maxCount, Double rate, String email) {
        return doctorClient.saveDoctor(doctorDto, maxCount, rate, email, UserRole.ADMIN.name());
    }

    private String saveDoctorFallback(DoctorDto doctorDto, Integer maxCount, Double rate, String email, Exception e) {
        System.out.println("saveDoctorProfile fallback triggered for: " + doctorDto.getEmail() + ". Error: " + e.getMessage());
        throw new RuntimeException("Doctor service unavailable, unable to save doctor profile for: " + doctorDto.getEmail());
    }

    @CircuitBreaker(name = "patientService", fallbackMethod = "savePatientFallback")
    @Retry(name = "patientService")
    public String savePatientProfile(PatientDto patientDto, String email) {
        return patientClient.savePatient(patientDto, email, UserRole.ADMIN.name());
    }

    private String savePatientFallback(PatientDto patientDto, String email, Exception e) {
        System.out.println("savePatientProfile fallback triggered for: " + patientDto.getEmail() + ". Error: " + e.getMessage());
        throw new RuntimeException("Patient service unavailable, unable to save patient profile for: " + patientDto.getEmail());
    }
}
