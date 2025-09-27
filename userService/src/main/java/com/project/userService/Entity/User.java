package com.project.userService.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String userId ;
    @Column(unique = true,nullable = false)
    private String userEmail;
    @Column(nullable = false)
    private String userPassword;
    private String UserName;
    private int userAge;
    @Enumerated(EnumType.STRING)
    private UserRole userRole=UserRole.USER;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime UpdatedAt;

}
