package com.project.userService.Controller;

import com.project.userService.Exceptions.NotFoundException;
import com.project.userService.Model.UserModel;
import com.project.userService.Service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user-service")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkConnection(){
        return  ResponseEntity.ok(Boolean.TRUE);
    }
    @PostMapping("/newUser")
    public ResponseEntity<UserModel> addNewUser(@Valid @RequestBody UserModel userModel){
        return ResponseEntity.ok(userService.addNewUser(userModel));
    }

    @GetMapping("/getUserId/{userId}")
    public ResponseEntity<UserModel> getExistingUserById(@PathVariable String userId){
        return ResponseEntity.ok(userService.findById(userId));
    }

    @GetMapping("/getEmailId/{emailId}")
    public ResponseEntity<UserModel> getExistingUserByEmailId(@PathVariable String emailId){
        return ResponseEntity.ok(userService.findByEmailId(emailId));
    }

    @GetMapping("/findUserId/{userId}")
    public ResponseEntity<Boolean> findExistingUserById(@PathVariable String userId){
        return ResponseEntity.ok(userService.existsById(userId));
    }

    @GetMapping("/findEmailId/{emailId}")
    public ResponseEntity<Boolean> findExistingUserByEmailId(@PathVariable String emailId){
        return ResponseEntity.ok(userService.existsByEmailId(emailId));
    }

    @PostMapping("/updateUser")
    public ResponseEntity<UserModel> updateById(@Valid @RequestBody UserModel userModel) throws NotFoundException {
        return ResponseEntity.ok(userService.updateByEmail(userModel));
    }

}
