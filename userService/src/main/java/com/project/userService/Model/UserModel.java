package com.project.userService.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.*;
@Data
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserModel {
    @NotBlank(message = "Email Field must be filled")
    @Email(message = "Enter correct email format")
    private String userEmail;
    @NotBlank(message = "Password is required")
    @Size(min=8,max=30,message = "Password length must be in range 8 to 30")
    private String userPassword;
    @NotBlank(message = "Name is required")
    private String userName;
    @NotNull(message = "Age is required")
    @Min(value = 10,message = "Age must be greater than 10")
    @Max(value = 100,message = "Age must be smaller than 100")
    private Integer userAge;
}
