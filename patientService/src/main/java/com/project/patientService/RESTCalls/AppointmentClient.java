package com.project.patientService.RESTCalls;

import com.project.patientService.Model.AppointmentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@FeignClient(name="appointment-service")
public interface AppointmentClient {

    @PutMapping("/api/appointment-service/secure/markCancelled/{appointmentId}")
    void markCancelled(
            @PathVariable String appointmentId,
            @RequestParam String cancelledBy,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @PutMapping("/api/appointment-service/secure/restoreAppointment/{appointmentId}")
    void restoreAppointment(
            @PathVariable String appointmentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @PostMapping("/api/appointment-service/secure/bookAppointment")
    AppointmentDto bookAppointment(
            @RequestBody AppointmentDto appointmentDto,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @DeleteMapping("/api/appointment-service/secure/deleteAppointment/{appointmentId}")
    void deleteAppointment(
            @PathVariable String appointmentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @GetMapping("/api/appointment-service/secure/getAppointmentsByUserId/{userId}")
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
