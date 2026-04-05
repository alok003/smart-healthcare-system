package com.project.adminService.Controller;

import com.project.adminService.Exceptions.IllegalRequestException;
import com.project.adminService.Exceptions.RequestNotFoundException;
import com.project.adminService.Exceptions.UnAuthorizedException;
import com.project.adminService.Model.RequestRoleDto;
import com.project.adminService.Service.AdminService;
import com.project.adminService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin-service/secure")
@AllArgsConstructor
public class AdminController {
    private AdminService adminService;
    private UtilityFunctions utilityFunctions;

    @GetMapping("/getAllRequests")
    public ResponseEntity<List<RequestRoleDto>> getAllActiveRequests(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllActiveRequests());
    }

    @PutMapping("/declineRequest/{id}")
    public ResponseEntity<String> declineRequest(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @PathVariable String id) throws UnAuthorizedException, RequestNotFoundException, IllegalRequestException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(adminService.declineRequest(id));
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<String> approveRequest(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @PathVariable String id, @RequestParam(defaultValue = "4") Integer maxCount, @RequestParam(defaultValue = "300") Double rate) throws UnAuthorizedException, RequestNotFoundException, IllegalRequestException {
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(adminService.approveRequest(id, email, maxCount, rate));
    }

    @PostMapping("/approve/patients")
    public ResponseEntity<Void> approvePatientRequest(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        adminService.approvePatientRequest();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/checkStatus")
    public ResponseEntity<RequestRoleDto> checkStatus(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @RequestParam String userEmail) throws UnAuthorizedException, RequestNotFoundException {
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(adminService.checkStatus(userEmail));
    }
}
