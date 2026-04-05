package com.project.userService.Service;

import com.project.userService.Entity.User;
import com.project.userService.Exceptions.InvalidRequestException;
import com.project.userService.Exceptions.UserNotFoundException;
import com.project.userService.Model.ChangeRequest;
import com.project.userService.Model.RequestRoleDto;
import com.project.userService.Model.UserModel;
import com.project.userService.Model.UserRole;
import com.project.userService.RESTCalls.AdminClient;
import com.project.userService.Repository.UserRepository;
import com.project.userService.Utility.UtilityFunction;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Map;

@Service
@AllArgsConstructor
public class UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UtilityFunction utilityFunction;
    private AdminClient adminClient;
    private KafkaTemplate<String, Map<String,Object>> kafkaTemplate;

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
        if (userRepository.existsByUserEmail(email)) {
            User user = userRepository.findByUserEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(email));
            user.setUserName(userModel.getUserName());
            user.setUserPassword(passwordEncoder.encode(userModel.getUserPassword()));
            user.setUserAge(userModel.getUserAge());
            User save = userRepository.save(user);
            return utilityFunction.cnvEntityToBean(save);
        } else throw new UserNotFoundException(email);
    }

    public String requestAdminAccess(RequestRoleDto requestRoleDto, String email, String role) {
        System.out.println("Admin access request from: " + email);
        requestRoleDto.setUserEmail(email);
        requestRoleDto.setUserRole(UserRole.ADMIN);
        try {
            kafkaTemplate.send("role-request", UtilityFunction.cnvDtoToMap(requestRoleDto)).get();
            System.out.println("Admin access request sent to Kafka for: " + email);
        } catch (Exception e) {
            System.out.println("Kafka send failed for admin request: " + email + ". Error: " + e.getMessage());
            throw new RuntimeException("Request failed, please try again later.");
        }
        return "Request for Admin access sent successfully";
    }

    public String requestDoctorAccess(RequestRoleDto requestRoleDto, String email, String role) throws InvalidRequestException {
        if (requestRoleDto.getDoctorDto() == null) throw new InvalidRequestException("doctorDto is required for Doctor access request");
        System.out.println("Doctor access request from: " + email);
        requestRoleDto.setUserEmail(email);
        requestRoleDto.setUserRole(UserRole.DOCTOR);
        requestRoleDto.getDoctorDto().setEmail(email);
        try {
            kafkaTemplate.send("role-request", UtilityFunction.cnvDtoToMap(requestRoleDto)).get();
            System.out.println("Doctor access request sent to Kafka for: " + email);
        } catch (Exception e) {
            System.out.println("Kafka send failed for doctor request: " + email + ". Error: " + e.getMessage());
            throw new RuntimeException("Request failed, please try again later.");
        }
        return "Request for Doctor access sent successfully";
    }

    public String requestPatientAccess(RequestRoleDto requestRoleDto, String email, String role) throws InvalidRequestException, UserNotFoundException {
        if (requestRoleDto.getPatientDto() == null) throw new InvalidRequestException("patientDto is required for Patient access request");
        System.out.println("Patient access request from: " + email);
        requestRoleDto.setUserEmail(email);
        requestRoleDto.setUserRole(UserRole.PATIENT);
        requestRoleDto.getPatientDto().setEmail(email);
        requestRoleDto.getPatientDto().setName(findByEmailId(email).getUserName());
        try {
            kafkaTemplate.send("role-request", UtilityFunction.cnvDtoToMap(requestRoleDto)).get();
            System.out.println("Patient access request sent to Kafka for: " + email);
        } catch (Exception e) {
            System.out.println("Kafka send failed for patient request: " + email + ". Error: " + e.getMessage());
            throw new RuntimeException("Request failed, please try again later.");
        }
        return "Request for Patient access sent successfully";
    }

    public String changeRole(ChangeRequest changeRequest) throws UserNotFoundException {
        User user = userRepository.findByUserEmail(changeRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException(changeRequest.getEmail()));
        user.setUserRole(changeRequest.getRole());
        user = userRepository.save(user);
        return changeRequest.getEmail();
    }

    @CircuitBreaker(name = "adminService", fallbackMethod = "checkStatusFallback")
    @Retry(name = "adminService")
    public RequestRoleDto checkStatus(String email) {
        return adminClient.checkStatusViaEmail(email, UserRole.ADMIN.name(), email);
    }

    private RequestRoleDto checkStatusFallback(String email, Exception e) {
        throw new RuntimeException("Admin service unavailable, unable to fetch status for: " + email);
    }
}
