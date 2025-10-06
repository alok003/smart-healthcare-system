package com.project.userService.Service;

import com.project.userService.Entity.User;
import com.project.userService.Exceptions.UserNotFoundException;
import com.project.userService.Model.RequestRoleDto;
import com.project.userService.Model.UserModel;
import com.project.userService.Model.UserRole;
import com.project.userService.RESTCalls.AdminClient;
import com.project.userService.Repository.UserRepository;
import com.project.userService.Utility.UtilityFunction;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

@Service
@AllArgsConstructor
public class UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UtilityFunction utilityFunction;
    private AdminClient adminClient;

    public UserModel findById(String userId) throws UserNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return utilityFunction.cnvEntityToBean(user);
    }

    public UserModel findByEmailId(String emailId) throws UserNotFoundException {
        User user = userRepository.findByUserEmail(emailId)
                .orElseThrow(UserNotFoundException::new);
        return utilityFunction.cnvEntityToBean(user);
    }

    public Boolean existsById(String userId) {
        return userRepository.existsByUserId(userId);
    }

    public Boolean existsByEmailId(String emailId) {
        return userRepository.existsByUserEmail(emailId);
    }

    public UserModel updateByEmail(@Valid UserModel userModel, String email) throws UserNotFoundException {
        if (userRepository.existsByUserEmail(email)) {
            User user = userRepository.findByUserEmail(userModel.getUserEmail()).get();
            user.setUserName(userModel.getUserName());
            user.setUserPassword(passwordEncoder.encode(userModel.getUserPassword()));
            user.setUserAge(userModel.getUserAge());
            User save = userRepository.save(user);
            return utilityFunction.cnvEntityToBean(save);
        } else throw new UserNotFoundException();
    }

    public RequestRoleDto requestAdminAccess(@Valid RequestRoleDto requestRoleDto, String email, String role) {
        requestRoleDto.setUserEmail(email);
        requestRoleDto.setUserRole(UserRole.ADMIN);
        return adminClient.saveRequestAdminClient(email,UserRole.ADMIN.name(),requestRoleDto);
    }

    public RequestRoleDto requestDoctorAccess(@Valid RequestRoleDto requestRoleDto, String email, String role) {
        requestRoleDto.setUserEmail(email);
        requestRoleDto.setUserRole(UserRole.DOCTOR);
        return adminClient.saveRequestAdminClient(email,UserRole.ADMIN.name(),requestRoleDto);
    }
}
