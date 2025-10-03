package com.project.userService.Controller;

import com.project.userService.Exceptions.UserAlreadyExistsException;
import com.project.userService.Model.AuthResponse;
import com.project.userService.Model.LoginRequest;
import com.project.userService.Model.UserModel;
import com.project.userService.Service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user-service/open")
@AllArgsConstructor
public class AuthController {

    private AuthService authService;

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkConnection() {
        return ResponseEntity.ok(Boolean.TRUE);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> createNewUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/newUser")
    public ResponseEntity<UserModel> addNewUser(@Valid @RequestBody UserModel userModel) throws UserAlreadyExistsException {
        return ResponseEntity.ok(authService.addNewUser(userModel));
    }
}
