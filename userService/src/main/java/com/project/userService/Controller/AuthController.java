package com.project.userService.Controller;

import com.project.userService.Exceptions.UserAlreadyExistsException;
import com.project.userService.Exceptions.UserNotFoundException;
import com.project.userService.Model.AuthResponse;
import com.project.userService.Model.LoginRequest;
import com.project.userService.Model.UserModel;
import com.project.userService.Service.AuthService;
import com.project.userService.Utility.LogUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-service/open")
@AllArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private AuthService authService;

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkConnection() {
        return ResponseEntity.status(HttpStatus.OK).body(Boolean.TRUE);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) throws UserNotFoundException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/user-service/open/login identifier={}", loginRequest.getUserEmail());
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(loginRequest));
    }

    @PostMapping("/newUser")
    public ResponseEntity<UserModel> addNewUser(@Valid @RequestBody UserModel userModel) throws UserAlreadyExistsException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/user-service/open/newUser payload={}", LogUtil.toJson(userModel));
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.addNewUser(userModel));
    }
}
