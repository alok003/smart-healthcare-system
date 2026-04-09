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
import com.project.userService.Utility.LogUtil;
import com.project.userService.Utility.UtilityFunction;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UtilityFunction utilityFunction;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public AuthResponse login(LoginRequest request) throws UserNotFoundException {
        log.info("action=USER_LOGIN status=INITIATED identifier={}", request.getUserEmail());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUserEmail(), request.getUserPassword()));
        User user = userRepository.findByUserEmail(request.getUserEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getUserEmail()));
        String token = jwtUtil.generateToken(user.getUserEmail(), user.getUserRole().name());
        log.info("action=USER_LOGIN status=SUCCESS identifier={} role={}", user.getUserEmail(), user.getUserRole());
        return AuthResponse.builder().token(token).expiration(jwtUtil.extractExpiration(token)).build();
    }

    public UserModel addNewUser(UserModel userModel) throws UserAlreadyExistsException {
        log.info("action=USER_REGISTER status=INITIATED identifier={} payload={}", userModel.getUserEmail(), LogUtil.toJson(userModel));
        if (userRepository.existsByUserEmail(userModel.getUserEmail())) {
            log.warn("action=USER_REGISTER status=REJECTED identifier={} reason=USER_ALREADY_EXISTS", userModel.getUserEmail());
            throw new UserAlreadyExistsException(userModel.getUserEmail());
        }
        User user = utilityFunction.cnvBeanToEntity(userModel);
        user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
        user.setUserRole(UserRole.USER);
        User save = userRepository.save(user);
        UserModel result = utilityFunction.cnvEntityToBean(save);
        result.setUserPassword(null);
        log.info("action=KAFKA_PUBLISH status=INITIATED topic=welcome-notification identifier={} payload={}", userModel.getUserEmail(), LogUtil.toJson(result));
        try {
            kafkaTemplate.send(MessageBuilder
                    .withPayload(UtilityFunction.cnvDtoToMap(result))
                    .setHeader(KafkaHeaders.TOPIC, "welcome-notification")
                    .setHeader("X-Correlation-ID", MDC.get("correlationId"))
                    .build()).get();
            log.info("action=KAFKA_PUBLISH status=SUCCESS topic=welcome-notification identifier={}", userModel.getUserEmail());
        } catch (Exception e) {
            log.error("action=KAFKA_PUBLISH status=FAILED topic=welcome-notification identifier={} reason=KAFKA_UNAVAILABLE detail=Registration rolled back error={}", userModel.getUserEmail(), e.getMessage());
            userRepository.delete(save);
            throw new RuntimeException("Registration failed due to notification service being unavailable. Please try again.");
        }
        log.info("action=USER_REGISTER status=SUCCESS identifier={} payload={}", userModel.getUserEmail(), LogUtil.toJson(result));
        return result;
    }
}
