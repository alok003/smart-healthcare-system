package com.project.adminService.RESTCalls;

import com.project.adminService.Model.ChangeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.validation.Valid;

@FeignClient(name = "user-service")
public interface UserClient {

    @PostMapping("/api/user-service/secure/changeRole")
    String changeRole(
            @RequestBody ChangeRequest changeRequest,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );
}
