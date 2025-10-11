package com.project.adminService.RESTCalls;

import com.project.adminService.Model.ChangeRequest;
import com.project.adminService.Model.DoctorDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "doctor-service")
public interface DoctorClient {

    @PostMapping("/api/doctor-service/secure/saveDoctor")
    String saveDoctor(
            @RequestBody DoctorDto doctorDto,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );
}
