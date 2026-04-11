package com.project.patientService.RESTCalls;

import com.project.patientService.Model.AppointmentDto;
import com.project.patientService.Model.DoctorDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "doctor-service")
public interface DoctorClient {

    @GetMapping("/api/doctor-service/secure/getAllDoctors")
    List<DoctorDto> getAllDoctorList(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @GetMapping("/api/doctor-service/secure/getDoctorByEmail/{doctorEmail}")
    DoctorDto getDoctorByEmail(
            @PathVariable String doctorEmail,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @PostMapping("/api/doctor-service/secure/addDocAppointment")
    Boolean addDocAppointment(
            @RequestBody AppointmentDto appointmentDto,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );
}
