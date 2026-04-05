package com.project.userService.Controller;

import com.project.userService.Exceptions.UserAlreadyExistsException;
import com.project.userService.Exceptions.UserNotFoundException;
import com.project.userService.Model.AuthResponse;
import com.project.userService.Model.LoginRequest;
import com.project.userService.Model.UserModel;
import com.project.userService.Service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user-service/open")
@AllArgsConstructor
public class AuthController {

    private AuthService authService;

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkConnection() {
        return ResponseEntity.status(HttpStatus.OK).body(Boolean.TRUE);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) throws UserNotFoundException {
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(loginRequest));
    }

    @PostMapping("/newUser")
    public ResponseEntity<UserModel> addNewUser(@Valid @RequestBody UserModel userModel) throws UserAlreadyExistsException {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.addNewUser(userModel));
    }
}
