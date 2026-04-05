package com.project.userService.RESTCalls;

import com.project.userService.Model.RequestRoleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="admin-service")
public interface AdminClient {

    @GetMapping("api/admin-service/secure/checkStatus")
    RequestRoleDto checkStatusViaEmail(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @RequestParam("userEmail") String userEmail
    );

}
