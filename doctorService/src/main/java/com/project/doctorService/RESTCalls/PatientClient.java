package com.project.doctorService.RESTCalls;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "patient-service")
public interface PatientClient {

    @DeleteMapping("/api/patient-service/secure/removeAppointment/{appointmentId}")
    void removeAppointmentFromPatient(
            @PathVariable String appointmentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );
}
