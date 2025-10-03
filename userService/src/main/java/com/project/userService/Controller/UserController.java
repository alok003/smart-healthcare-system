package com.project.userService.Controller;

import com.project.userService.Exceptions.UserNotFoundException;
import com.project.userService.Exceptions.UnAuthorizedException;
import com.project.userService.Model.UserModel;
import com.project.userService.Service.UserService;
import com.project.userService.Utility.UtilityFunction;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user-service/secure")
@AllArgsConstructor
public class UserController {

    private UserService userService;
    private UtilityFunction utilityFunction;

    @GetMapping("/getUserId/{userId}")
    public ResponseEntity<UserModel> getExistingUserById(@PathVariable String userId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, UserNotFoundException {
        if (!utilityFunction.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(userService.findById(userId));
    }

    @GetMapping("/getEmailId/{emailId}")
    public ResponseEntity<UserModel> getExistingUserByEmailId(@PathVariable String emailId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, UserNotFoundException {
        if (!utilityFunction.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(userService.findByEmailId(emailId));
    }

    @GetMapping("/findUserId/{userId}")
    public ResponseEntity<Boolean> findExistingUserById(@PathVariable String userId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if (!utilityFunction.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(userService.existsById(userId));
    }

    @GetMapping("/findEmailId/{emailId}")
    public ResponseEntity<Boolean> findExistingUserByEmailId(@PathVariable String emailId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if (!utilityFunction.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(userService.existsByEmailId(emailId));
    }

    @PostMapping("/updateUser")
    public ResponseEntity<UserModel> updateById(@Valid @RequestBody UserModel userModel, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, UserNotFoundException {
        if (!utilityFunction.validateRequestUser(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(userService.updateByEmail(userModel, email));
    }

}
