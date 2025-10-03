package com.project.userService.Service;

import com.project.userService.Entity.User;
import com.project.userService.Exceptions.UserAlreadyExistsException;
import com.project.userService.Model.AuthResponse;
import com.project.userService.Model.LoginRequest;
import com.project.userService.Model.UserModel;
import com.project.userService.Repository.UserRepository;
import com.project.userService.Utility.JWTUtil;
import com.project.userService.Utility.UtilityFunction;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

@Service
@AllArgsConstructor
public class AuthService {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UtilityFunction utilityFunction;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken
                (request.getUserEmail(), request.getUserPassword()));
        User user = userRepository.findByUserEmail(request.getUserEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found!!!"));
        String token = jwtUtil.generateToken(user.getUserEmail(), user.getUserRole().name());
        return AuthResponse.builder().token(token).expiration(jwtUtil.extractExpiration(token)).build();
    }

    public UserModel addNewUser(@Valid UserModel userModel) throws UserAlreadyExistsException {
        if (userRepository.existsByUserEmail(userModel.getUserEmail())) {
            throw new UserAlreadyExistsException();
        }
        User user = utilityFunction.cnvBeanToEntity(userModel);
        user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
        User save = userRepository.save(user);
        return utilityFunction.cnvEntityToBean(save);
    }

}
