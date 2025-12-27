package com.project.adminService.Repository;

import com.project.adminService.Entity.RequestRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends MongoRepository<RequestRole,String> {
    Optional<RequestRole> findByUserEmail(String email);
}
