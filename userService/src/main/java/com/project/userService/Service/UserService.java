package com.project.userService.Service;

import com.project.userService.Entity.User;
import com.project.userService.Exceptions.InvalidRequestException;
import com.project.userService.Exceptions.UserNotFoundException;
import com.project.userService.Model.ChangeRequest;
import com.project.userService.Model.RequestRoleDto;
import com.project.userService.Model.UserModel;
import com.project.userService.Model.UserRole;
import com.project.userService.Repository.UserRepository;
import com.project.userService.Utility.LogUtil;
import com.project.userService.Utility.UtilityFunction;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UtilityFunction utilityFunction;
    private ExternalServiceClient externalServiceClient;
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public UserModel findById(String userId) throws UserNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return utilityFunction.cnvEntityToBean(user);
    }

    public UserModel findByEmailId(String emailId) throws UserNotFoundException {
        User user = userRepository.findByUserEmail(emailId)
                .orElseThrow(() -> new UserNotFoundException(emailId));
        return utilityFunction.cnvEntityToBean(user);
    }

    public Boolean existsById(String userId) {
        return userRepository.existsByUserId(userId);
    }

    public Boolean existsByEmailId(String emailId) {
        return userRepository.existsByUserEmail(emailId);
    }

    public UserModel updateByEmail(UserModel userModel, String email) throws UserNotFoundException {
        log.info("action=USER_UPDATE status=INITIATED identifier={} payload={}", email, LogUtil.toJson(userModel));
        User user = userRepository.findByUserEmail(email).orElseThrow(() -> new UserNotFoundException(email));
        user.setUserName(userModel.getUserName());
        user.setUserPassword(passwordEncoder.encode(userModel.getUserPassword()));
        user.setUserAge(userModel.getUserAge());
        User save = userRepository.save(user);
        UserModel result = utilityFunction.cnvEntityToBean(save);
        log.info("action=USER_UPDATE status=SUCCESS identifier={} payload={}", email, LogUtil.toJson(result));
        return result;
    }

    public String requestAdminAccess(RequestRoleDto requestRoleDto, String email, String role) {
        log.info("action=ROLE_REQUEST status=INITIATED identifier={} role={} targetRole=ADMIN payload={}", email, role, LogUtil.toJson(requestRoleDto));
        requestRoleDto.setUserEmail(email);
        requestRoleDto.setUserRole(UserRole.ADMIN);
        log.info("action=KAFKA_PUBLISH status=INITIATED topic=role-request identifier={} payload={}", email, LogUtil.toJson(requestRoleDto));
        try {
            kafkaTemplate.send(MessageBuilder
                    .withPayload(UtilityFunction.cnvDtoToMap(requestRoleDto))
                    .setHeader(KafkaHeaders.TOPIC, "role-request")
                    .setHeader("X-Correlation-ID", MDC.get("correlationId"))
                    .build()).get();
            log.info("action=KAFKA_PUBLISH status=SUCCESS topic=role-request identifier={}", email);
        } catch (Exception e) {
            log.error("action=KAFKA_PUBLISH status=FAILED topic=role-request identifier={} reason=KAFKA_UNAVAILABLE error={}", email, e.getMessage());
            throw new RuntimeException("Request failed, please try again later.");
        }
        log.info("action=ROLE_REQUEST status=SUCCESS identifier={} targetRole=ADMIN", email);
        return "Request for Admin access sent successfully";
    }

    public String requestDoctorAccess(RequestRoleDto requestRoleDto, String email, String role) throws InvalidRequestException {
        if (requestRoleDto.getDoctorDto() == null) {
            log.warn("action=ROLE_REQUEST status=REJECTED identifier={} role={} reason=DOCTOR_DTO_MISSING", email, role);
            throw new InvalidRequestException("doctorDto is required for Doctor access request");
        }
        log.info("action=ROLE_REQUEST status=INITIATED identifier={} role={} targetRole=DOCTOR payload={}", email, role, LogUtil.toJson(requestRoleDto));
        requestRoleDto.setUserEmail(email);
        requestRoleDto.setUserRole(UserRole.DOCTOR);
        requestRoleDto.getDoctorDto().setEmail(email);
        log.info("action=KAFKA_PUBLISH status=INITIATED topic=role-request identifier={} payload={}", email, LogUtil.toJson(requestRoleDto));
        try {
            kafkaTemplate.send(MessageBuilder
                    .withPayload(UtilityFunction.cnvDtoToMap(requestRoleDto))
                    .setHeader(KafkaHeaders.TOPIC, "role-request")
                    .setHeader("X-Correlation-ID", MDC.get("correlationId"))
                    .build()).get();
            log.info("action=KAFKA_PUBLISH status=SUCCESS topic=role-request identifier={}", email);
        } catch (Exception e) {
            log.error("action=KAFKA_PUBLISH status=FAILED topic=role-request identifier={} reason=KAFKA_UNAVAILABLE error={}", email, e.getMessage());
            throw new RuntimeException("Request failed, please try again later.");
        }
        log.info("action=ROLE_REQUEST status=SUCCESS identifier={} targetRole=DOCTOR", email);
        return "Request for Doctor access sent successfully";
    }

    public String requestPatientAccess(RequestRoleDto requestRoleDto, String email, String role) throws InvalidRequestException, UserNotFoundException {
        if (requestRoleDto.getPatientDto() == null) {
            log.warn("action=ROLE_REQUEST status=REJECTED identifier={} role={} reason=PATIENT_DTO_MISSING", email, role);
            throw new InvalidRequestException("patientDto is required for Patient access request");
        }
        log.info("action=ROLE_REQUEST status=INITIATED identifier={} role={} targetRole=PATIENT payload={}", email, role, LogUtil.toJson(requestRoleDto));
        requestRoleDto.setUserEmail(email);
        requestRoleDto.setUserRole(UserRole.PATIENT);
        requestRoleDto.getPatientDto().setEmail(email);
        requestRoleDto.getPatientDto().setName(findByEmailId(email).getUserName());
        log.info("action=KAFKA_PUBLISH status=INITIATED topic=role-request identifier={} payload={}", email, LogUtil.toJson(requestRoleDto));
        try {
            kafkaTemplate.send(MessageBuilder
                    .withPayload(UtilityFunction.cnvDtoToMap(requestRoleDto))
                    .setHeader(KafkaHeaders.TOPIC, "role-request")
                    .setHeader("X-Correlation-ID", MDC.get("correlationId"))
                    .build()).get();
            log.info("action=KAFKA_PUBLISH status=SUCCESS topic=role-request identifier={}", email);
        } catch (Exception e) {
            log.error("action=KAFKA_PUBLISH status=FAILED topic=role-request identifier={} reason=KAFKA_UNAVAILABLE error={}", email, e.getMessage());
            throw new RuntimeException("Request failed, please try again later.");
        }
        log.info("action=ROLE_REQUEST status=SUCCESS identifier={} targetRole=PATIENT", email);
        return "Request for Patient access sent successfully";
    }

    public String changeRole(ChangeRequest changeRequest) throws UserNotFoundException {
        log.info("action=ROLE_CHANGE status=INITIATED identifier={} targetRole={} payload={}", changeRequest.getEmail(), changeRequest.getRole(), LogUtil.toJson(changeRequest));
        User user = userRepository.findByUserEmail(changeRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException(changeRequest.getEmail()));
        user.setUserRole(changeRequest.getRole());
        userRepository.save(user);
        log.info("action=ROLE_CHANGE status=SUCCESS identifier={} targetRole={}", changeRequest.getEmail(), changeRequest.getRole());
        return changeRequest.getEmail();
    }

    public RequestRoleDto checkStatus(String email) {
        return externalServiceClient.checkStatus(email);
    }
}
