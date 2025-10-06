package com.project.userService.RESTCalls;

import com.project.userService.Model.RequestRoleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="admin-service")
public interface AdminClient {

    @PostMapping("/api/admin-service/secure/saveRequest")
    RequestRoleDto saveRequestAdminClient(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @RequestBody RequestRoleDto requestRoleDto
    );
}
