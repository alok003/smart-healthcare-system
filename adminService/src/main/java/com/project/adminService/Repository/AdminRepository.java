package com.project.adminService.Repository;

import com.project.adminService.Entity.RequestRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends MongoRepository<RequestRole,String> {

}
