package com.project.adminService.Controller;

import com.project.adminService.Exceptions.IllegalRequestException;
import com.project.adminService.Exceptions.RequestNotFoundException;
import com.project.adminService.Exceptions.UnAuthorizedException;
import com.project.adminService.Model.RequestRoleDto;
import com.project.adminService.Service.AdminService;
import com.project.adminService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin-service/secure")
@AllArgsConstructor
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private AdminService adminService;
    private UtilityFunctions utilityFunctions;

    @GetMapping("/getAllRequests")
    public ResponseEntity<List<RequestRoleDto>> getAllActiveRequests(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/admin-service/secure/getAllRequests requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllActiveRequests());
    }

    @PutMapping("/declineRequest/{id}")
    public ResponseEntity<String> declineRequest(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @PathVariable String id) throws UnAuthorizedException, RequestNotFoundException, IllegalRequestException {
        log.info("action=REQUEST_RECEIVED method=PUT path=/api/admin-service/secure/declineRequest/{} requestedBy={} role={}", id, email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(adminService.declineRequest(id));
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<String> approveRequest(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @PathVariable String id, @RequestParam(defaultValue = "4") Integer maxCount, @RequestParam(defaultValue = "300") Double rate) throws UnAuthorizedException, RequestNotFoundException, IllegalRequestException {
        log.info("action=REQUEST_RECEIVED method=PUT path=/api/admin-service/secure/approve/{} requestedBy={} role={} maxCount={} rate={}", id, email, role, maxCount, rate);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(adminService.approveRequest(id, email, maxCount, rate));
    }

    @PostMapping("/approve/patients")
    public ResponseEntity<Void> approvePatientRequest(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/admin-service/secure/approve/patients requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        adminService.approvePatientRequest();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/checkStatus")
    public ResponseEntity<RequestRoleDto> checkStatus(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @RequestParam String userEmail) throws UnAuthorizedException, RequestNotFoundException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/admin-service/secure/checkStatus requestedBy={} role={} identifier={}", email, role, userEmail);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(adminService.checkStatus(userEmail));
    }
}
