package com.project.patientService.RESTCalls;

import com.project.patientService.Model.AppointmentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@FeignClient(name="appointment-service")
public interface AppointmentClient {

    @PostMapping("/api/appointment-service/secure/bookAppointment")
    AppointmentDto bookAppointment(
            @RequestBody AppointmentDto appointmentDto,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @PostMapping("/api/appointment-service/secure/cancelAppointment")
    AppointmentDto cancelAppointmentAppointmentClient(
            @RequestBody String AppointmentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @GetMapping("/api/appointment-service/secure//getAppointmentsByUserId/{userId}")
    List<AppointmentDto> getAllAppointmentsbyPatient(
            @PathVariable String userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @GetMapping("/api/appointment-service/secure/getAppointment/{appointmentId}")
    AppointmentDto getAppointmentById(
            @PathVariable String appointmentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );
}
