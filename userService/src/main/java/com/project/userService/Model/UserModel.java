package com.project.userService.Model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.*;
@Data
@RequiredArgsConstructor
public class UserModel {
    @NotBlank(message = "Email Field must be filled")
    @Email(message = "Enter correct email format")
    private String userEmail;
    @NotBlank(message = "Field must not be Blank")
    @Size(min=3,max=10,message = "length must be in range 3 to 10")
    private String userPassword;
    @NotBlank(message = "Field must not be Blank")
    private String userName;
    @NotNull(message = "Field must not be Blank")
    @Min(value = 10,message = "Age must be greater than 10")
    @Max(value = 100,message = "Age must be smaller than 100")
    private int userAge;
}
