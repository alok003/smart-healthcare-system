package com.project.adminService.Repository;

import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Model.Status;
import com.project.adminService.Model.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends MongoRepository<RequestRole,String> {
    Optional<RequestRole> findByUserEmail(String email);

    List<RequestRole> findByUserRoleAndRequestStatus(UserRole userRole, Status status);

    List<RequestRole> findByRequestStatus(Status status);
}
