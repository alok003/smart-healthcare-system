package com.project.patientService.Repository;

import com.project.patientService.Entity.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends MongoRepository<Patient,String> {
    Optional<Patient> findByEmail(String email);
}
