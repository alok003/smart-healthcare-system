package com.project.adminService.Controller;

import com.project.adminService.Exceptions.RequestNotFoundException;
import com.project.adminService.Exceptions.UnAuthorizedException;
import com.project.adminService.Model.RequestRoleDto;
import com.project.adminService.Service.AdminService;
import com.project.adminService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
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
    public ResponseEntity<List<RequestRoleDto>> getAllRequests(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(adminService.getAllRequests());
    }

    @PostMapping("/saveRequest")
    public ResponseEntity<RequestRoleDto> saveRequest(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role,@RequestBody RequestRoleDto requestRoleDto) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(adminService.saveRequest(requestRoleDto));
    }

    @DeleteMapping("/deleteRequest/{id}")
    public ResponseEntity<String> deleteRequest(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role,@PathVariable String id) throws UnAuthorizedException, RequestNotFoundException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(adminService.declineRequest(id));
    }

    @GetMapping("/approve/{id}")
    public ResponseEntity<String> approveRequest(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role,@PathVariable String id) throws UnAuthorizedException, RequestNotFoundException {
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(adminService.approveRequest(id,email));
    }

    @GetMapping("/checkStatus")
    public ResponseEntity<RequestRoleDto> checkStatus(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, RequestNotFoundException {
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(adminService.checkStatus(email));
    }
}
