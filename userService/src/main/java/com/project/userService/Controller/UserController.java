package com.project.userService.Controller;

import com.project.userService.Exceptions.InvalidRequestException;
import com.project.userService.Exceptions.UserNotFoundException;
import com.project.userService.Exceptions.UnAuthorizedException;
import com.project.userService.Model.ChangeRequest;
import com.project.userService.Model.RequestRoleDto;
import com.project.userService.Model.UserModel;
import com.project.userService.Service.UserService;
import com.project.userService.Utility.UtilityFunction;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user-service/secure")
@AllArgsConstructor
public class UserController {

    private UserService userService;
    private UtilityFunction utilityFunction;

    @GetMapping("/getUserId/{userId}")
    public ResponseEntity<UserModel> getExistingUserById(@PathVariable String userId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, UserNotFoundException {
        if (!utilityFunction.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(userService.findById(userId));
    }

    @GetMapping("/getEmailId/{emailId}")
    public ResponseEntity<UserModel> getExistingUserByEmailId(@PathVariable String emailId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, UserNotFoundException {
        if (!utilityFunction.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(userService.findByEmailId(emailId));
    }

    @GetMapping("/findUserId/{userId}")
    public ResponseEntity<Boolean> findExistingUserById(@PathVariable String userId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if (!utilityFunction.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(userService.existsById(userId));
    }

    @GetMapping("/findEmailId/{emailId}")
    public ResponseEntity<Boolean> findExistingUserByEmailId(@PathVariable String emailId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if (!utilityFunction.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(userService.existsByEmailId(emailId));
    }

    @PostMapping("/updateUser")
    public ResponseEntity<UserModel> updateById(@Valid @RequestBody UserModel userModel, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, UserNotFoundException {
        if (!utilityFunction.validateRequestUser(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateByEmail(userModel, email));
    }

    @PostMapping("/requestAdminAccess")
    public ResponseEntity<String> requestAdminAccess(@Valid @RequestBody RequestRoleDto requestRoleDto,@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if (!utilityFunction.validateRequestUser(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(userService.requestAdminAccess(requestRoleDto,email,role));
    }

    @PostMapping("/requestDoctorAccess")
    public ResponseEntity<String> requestDoctorAccess(@Valid @RequestBody RequestRoleDto requestRoleDto,@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, InvalidRequestException {
        if (!utilityFunction.validateRequestUser(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(userService.requestDoctorAccess(requestRoleDto,email,role));
    }

    @PostMapping("/requestPatientAccess")
    public ResponseEntity<String> requestPatientAccess(@Valid @RequestBody RequestRoleDto requestRoleDto,@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, InvalidRequestException, UserNotFoundException {
        if (!utilityFunction.validateRequestUser(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(userService.requestPatientAccess(requestRoleDto,email,role));
    }

    @PostMapping("/changeRole")
    public ResponseEntity<String> changeRole(@Valid @RequestBody ChangeRequest changeRequest, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, UserNotFoundException {
        if (!utilityFunction.validateRequestAdmin(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(userService.changeRole(changeRequest));
    }

    @GetMapping("/checkStatus")
    public ResponseEntity<RequestRoleDto> checkStatus(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException{
        if (!utilityFunction.validateRequestUser(email, role)) throw new UnAuthorizedException(email);
        return ResponseEntity.status(HttpStatus.OK).body(userService.checkStatus(email));
    }
}
