package com.project.userService.Utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userService.Entity.User;
import com.project.userService.Model.RequestRoleDto;
import com.project.userService.Model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class UtilityFunction {

    private static final ObjectMapper objectMapper=new ObjectMapper();

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

    public static <T> Map<String, Object> cnvDtoToMap(T dto) {
        return objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {});
    }

    public static <T> T cnvMapToDto(Map<String, Object> map, Class<T> clazz) {
        return objectMapper.convertValue(map, clazz);
    }

    private boolean validateEmail(String email) {
        return email != null && email.contains("@");
    }
}
