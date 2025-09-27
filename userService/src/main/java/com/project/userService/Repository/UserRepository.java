package com.project.userService.Repository;

import com.project.userService.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,String> {

    Optional<User> findByUserEmail(String email);
    Boolean existsByUserId(String id);
    Boolean existsByUserEmail(String id);

}
