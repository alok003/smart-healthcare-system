package com.project.adminService.RESTCalls;

import com.project.adminService.Model.ChangeRequest;
import com.project.adminService.Model.DoctorDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "doctor-service")
public interface DoctorClient {

    @PostMapping("/api/doctor-service/secure/saveDoctor")
    String saveDoctor(
            @RequestBody DoctorDto doctorDto,
            @RequestParam int maxCount,
            @RequestParam double rate,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );
}
