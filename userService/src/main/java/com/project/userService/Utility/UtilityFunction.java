package com.project.userService.Utility;

import com.project.userService.Entity.User;
import com.project.userService.Model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UtilityFunction {
    public User cnvBeanToEntity(UserModel userModel) {
        User user = new User();
        BeanUtils.copyProperties(userModel, user);
        return user;
    }

    public UserModel cnvEntityToBean(User user) {
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(user, userModel);
        return userModel;
    }

    public Boolean validateRequestAdmin(String email,String role){
        return Objects.equals(role, "ADMIN")&&validateEmail(email);
    }

    public Boolean validateRequestDoctor(String email,String role){
        return Objects.equals(role, "DOCTOR")&&validateEmail(email);
    }

    public Boolean validateRequestPatient(String email,String role){
        return Objects.equals(role, "PATIENT")&&validateEmail(email);
    }

    public Boolean validateRequestUser(String email,String role){
        return Objects.equals(role, "USER")&&validateEmail(email);
    }

    private boolean validateEmail(String email) {
        return email != null && email.contains("@");
    }
}
