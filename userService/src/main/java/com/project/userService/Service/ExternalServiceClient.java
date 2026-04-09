package com.project.userService.Service;

import com.project.userService.Model.RequestRoleDto;
import com.project.userService.Model.UserRole;
import com.project.userService.RESTCalls.AdminClient;
import com.project.userService.Utility.LogUtil;
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

    private AdminClient adminClient;

    @CircuitBreaker(name = "adminService", fallbackMethod = "checkStatusFallback")
    @Retry(name = "adminService")
    public RequestRoleDto checkStatus(String email) {
        log.info("action=FEIGN_REQUEST service=admin-service endpoint=/api/admin-service/secure/checkStatus identifier={}", email);
        RequestRoleDto result = adminClient.checkStatusViaEmail(email, UserRole.ADMIN.name(), email);
        log.info("action=FEIGN_RESPONSE service=admin-service status=SUCCESS identifier={} payload={}", email, LogUtil.toJson(result));
        return result;
    }

    private RequestRoleDto checkStatusFallback(String email, Exception e) {
        log.error("action=FEIGN_RESPONSE service=admin-service status=FAILED identifier={} reason=SERVICE_UNAVAILABLE error={}", email, e.getMessage());
        throw new RuntimeException("Admin service unavailable, unable to fetch status for: " + email);
    }
}
