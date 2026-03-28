package com.project.appointmentService.Repository;

import com.project.appointmentService.Entity.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment,String> {
    List<Appointment> findByPatientId(String userId);

    List<Appointment> findByDoctorId(String doctorId);
}
    