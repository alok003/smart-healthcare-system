package com.project.adminService.RESTCalls;

import com.project.adminService.Model.DoctorDto;
import com.project.adminService.Model.PatientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="patient-service")
public interface PatientClient {

    @PostMapping("/api/patient-service/secure/savePatient")
    String savePatient(
            @RequestBody PatientDto patientDto,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );
}
