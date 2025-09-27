package com.project.userService.Service;

import com.project.userService.Entity.User;
import com.project.userService.Exceptions.NotFoundException;
import com.project.userService.Model.UserModel;
import com.project.userService.Repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {

    private  UserRepository userRepository;

    public UserModel addNewUser(@Valid UserModel userModel) {
        User user=new User();
        if(userRepository.existsByUserEmail(userModel.getUserEmail())){
            user=userRepository.findByUserEmail(userModel.getUserEmail()).get();
            user.setUserAge(userModel.getUserAge());
            user.setUserPassword(userModel.getUserPassword());
            user.setUserName(userModel.getUserName());
        }else{
            user=cnvBeanToEntity(userModel);
        }
        User save = userRepository.save(user);
        return cnvEntityToBean(save);
    }

    public UserModel findById(String userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User Not Found"));
        return cnvEntityToBean(user);
    }

    public UserModel findByEmailId(String emailId) {
        User user=userRepository.findByUserEmail(emailId)
                .orElseThrow(()->new RuntimeException("User not found"));
        return cnvEntityToBean(user);
    }

    public Boolean existsById(String userId) {
        return userRepository.existsByUserId(userId);
    }

    public Boolean existsByEmailId(String emailId) {
        return userRepository.existsByUserEmail(emailId);
    }

    public UserModel updateByEmail(@Valid UserModel userModel) throws NotFoundException {
        if(userRepository.existsByUserEmail(userModel.getUserEmail())){
            User user=userRepository.findByUserEmail(userModel.getUserEmail()).get();
            user.setUserName(userModel.getUserName());
            user.setUserPassword(userModel.getUserPassword());
            user.setUserAge(userModel.getUserAge());
            User save=userRepository.save(user);
            return cnvEntityToBean(save);
        }else throw new NotFoundException();
    }

    private User cnvBeanToEntity(UserModel userModel){
        User user =new User();
        BeanUtils.copyProperties(userModel,user);
        return user;
    }

    private UserModel cnvEntityToBean(User user){
        UserModel userModel =new UserModel();
        BeanUtils.copyProperties(user,userModel);
        return userModel;
    }
}
