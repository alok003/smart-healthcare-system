package com.project.appointmentService.Controller;


import com.project.appointmentService.Exception.AppointmentNotFoundException;
import com.project.appointmentService.Exception.UnAuthorizedException;
import com.project.appointmentService.Model.AppointmentDto;
import com.project.appointmentService.Model.VisitDetails;
import com.project.appointmentService.Service.AppointmentService;
import com.project.appointmentService.Utility.UtilityFunctions;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/appointment-service/secure")
public class AppointmentController {

    private AppointmentService appointmentService;
    private UtilityFunctions utilityFunctions;

    @PostMapping("/bookAppointment")
    public ResponseEntity<AppointmentDto> bookAppointment(@Valid @RequestBody AppointmentDto appointmentDto, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email,role))throw new UnAuthorizedException();
        return ResponseEntity.ok(appointmentService.saveAppointment(appointmentDto));
    }

    @DeleteMapping("/cancelAppointment")
    public ResponseEntity<AppointmentDto> cancelAppointment(String appointmentId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, AppointmentNotFoundException {
        if(!utilityFunctions.validateRequestAdmin(email,role))throw new UnAuthorizedException();
        return ResponseEntity.ok(appointmentService.cancelAppointment(appointmentId));
    }

    @PostMapping("/completeAppointment")
    public ResponseEntity<AppointmentDto> completeAppointment(@Valid @RequestBody VisitDetails visitDetails, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, AppointmentNotFoundException {
        if(!utilityFunctions.validateRequestAdmin(email, role))throw new UnAuthorizedException();
        return ResponseEntity.ok(appointmentService.completeAppointment(visitDetails));
    }

    @GetMapping("/getAppointment/{appointmentId}")
    public ResponseEntity<AppointmentDto> getAppointment(@PathVariable String appointmentId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, AppointmentNotFoundException {
        if(!utilityFunctions.validateRequestAdmin(email, role))throw new UnAuthorizedException();
        return ResponseEntity.ok(appointmentService.getAppointmentById(appointmentId));
    }

    @GetMapping("/getAllAppointments")
    public ResponseEntity<List<AppointmentDto>> getAllAppointments(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role))throw new UnAuthorizedException();
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/getAppointmentsByUserId/{userId}")
    public ResponseEntity<List<AppointmentDto>> getAppointmentsByUserId(@RequestParam String userId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role))throw new UnAuthorizedException();
        return ResponseEntity.ok(appointmentService.getAppointmentsByUserId(userId));
    }

    @GetMapping("/getAppointmentsByDoctorId/{doctorId}")
    public ResponseEntity<List<AppointmentDto>> getAppointmentsByDoctorId(@RequestParam String doctorId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role))throw new UnAuthorizedException();
        return ResponseEntity.ok(appointmentService.getAppointmentsByDoctorId(doctorId));
    }
}
