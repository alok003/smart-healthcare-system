package com.project.userService.Service;

import com.project.userService.Entity.User;
import com.project.userService.Exceptions.UserAlreadyExistsException;
import com.project.userService.Exceptions.UserNotFoundException;
import com.project.userService.Model.AuthResponse;
import com.project.userService.Model.LoginRequest;
import com.project.userService.Model.UserModel;
import com.project.userService.Model.UserRole;
import com.project.userService.Repository.UserRepository;
import com.project.userService.Utility.JWTUtil;
import com.project.userService.Utility.UtilityFunction;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class AuthService {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UtilityFunction utilityFunction;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public AuthResponse login(LoginRequest request) throws UserNotFoundException {
        System.out.println("Login attempt for: " + request.getUserEmail());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken
                (request.getUserEmail(), request.getUserPassword()));
        User user = userRepository.findByUserEmail(request.getUserEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getUserEmail()));
        String token = jwtUtil.generateToken(user.getUserEmail(), user.getUserRole().name());
        System.out.println("Login successful for: " + request.getUserEmail());
        return AuthResponse.builder().token(token).expiration(jwtUtil.extractExpiration(token)).build();
    }

    public UserModel addNewUser(UserModel userModel) throws UserAlreadyExistsException {
        System.out.println("Registering new user: " + userModel.getUserEmail());
        if (userRepository.existsByUserEmail(userModel.getUserEmail())) {
            throw new UserAlreadyExistsException(userModel.getUserEmail());
        }
        User user = utilityFunction.cnvBeanToEntity(userModel);
        user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
        user.setUserRole(UserRole.USER);
        User save = userRepository.save(user);
        UserModel result = utilityFunction.cnvEntityToBean(save);
        result.setUserPassword(null);
        try {
            kafkaTemplate.send("welcome-notification", UtilityFunction.cnvDtoToMap(result)).get();
            System.out.println("Welcome notification sent for: " + userModel.getUserEmail());
        } catch (Exception e) {
            System.out.println("Welcome notification failed for: " + userModel.getUserEmail() + ", rolling back. Error: " + e.getMessage());
            userRepository.delete(save);
            throw new RuntimeException("Registration failed due to notification service being unavailable. Please try again.");
        }
        return result;
    }

}