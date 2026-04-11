package com.project.userService.Model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@RequiredArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email Field must be filled")
    @Email(message = "Enter correct email format")
    private String userEmail;
    @NotBlank(message = "Field must not be Blank")
    @Size(min = 8, max = 30, message = "Password length must be in range 8 to 30")
    private String userPassword;
}
