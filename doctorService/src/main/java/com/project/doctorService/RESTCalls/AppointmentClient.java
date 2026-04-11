package com.project.doctorService.RESTCalls;

import com.project.doctorService.Model.AppointmentDto;
import com.project.doctorService.Model.VisitDetails;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "appointment-service")
public interface AppointmentClient {

    @DeleteMapping("/api/appointment-service/secure/cancelAppointment/{appointmentId}")
    AppointmentDto cancelAppointmentAppointmentClient(
            @PathVariable String appointmentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @PostMapping("/api/appointment-service/secure/completeAppointment")
    AppointmentDto completeAppointment(
            @RequestBody VisitDetails visitDetails,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @GetMapping("/api/appointment-service/secure/getAppointment/{appointmentId}")
    AppointmentDto getAppointmentById(
            @PathVariable String appointmentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );

    @GetMapping("/api/appointment-service/secure/getAppointmentsByDoctorId/{doctorId}")
    List<AppointmentDto> getAppointmentsByDoctorId(
            @PathVariable String doctorId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );
}
