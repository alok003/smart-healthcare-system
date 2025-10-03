package com.project.userService.Model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@RequiredArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email Field must be filled")
    @Email(message = "Enter correct email format")
    private String userEmail;
    @NotBlank(message = "Field must not be Blank")
    @Size(min = 3, max = 10, message = "length must be in range 3 to 10")
    private String userPassword;
}
