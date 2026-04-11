package com.project.appointmentService.Controller;

import com.project.appointmentService.Exceptions.AppointmentNotFoundException;
import com.project.appointmentService.Exceptions.UnAuthorizedException;
import com.project.appointmentService.Model.AppointmentDto;
import com.project.appointmentService.Model.VisitDetails;
import com.project.appointmentService.Service.AppointmentService;
import com.project.appointmentService.Utility.UtilityFunctions;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/appointment-service/secure")
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private AppointmentService appointmentService;
    private UtilityFunctions utilityFunctions;

    @PostMapping("/bookAppointment")
    public ResponseEntity<AppointmentDto> bookAppointment(@Valid @RequestBody AppointmentDto appointmentDto, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/appointment-service/secure/bookAppointment requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.saveAppointment(appointmentDto));
    }

    @PutMapping("/markCancelled/{appointmentId}")
    public ResponseEntity<Void> markCancelled(@PathVariable String appointmentId, @RequestParam String cancelledBy, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, AppointmentNotFoundException {
        log.info("action=REQUEST_RECEIVED method=PUT path=/api/appointment-service/secure/markCancelled/{} requestedBy={} role={} cancelledBy={}", appointmentId, email, role, cancelledBy);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        appointmentService.markCancelled(appointmentId, cancelledBy);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/restoreAppointment/{appointmentId}")
    public ResponseEntity<Void> restoreAppointment(@PathVariable String appointmentId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, AppointmentNotFoundException {
        log.info("action=REQUEST_RECEIVED method=PUT path=/api/appointment-service/secure/restoreAppointment/{} requestedBy={} role={}", appointmentId, email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        appointmentService.restoreAppointment(appointmentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/deleteAppointment/{appointmentId}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String appointmentId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, AppointmentNotFoundException {
        log.info("action=REQUEST_RECEIVED method=DELETE path=/api/appointment-service/secure/deleteAppointment/{} requestedBy={} role={}", appointmentId, email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        appointmentService.deleteAppointment(appointmentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/completeAppointment")
    public ResponseEntity<AppointmentDto> completeAppointment(@Valid @RequestBody VisitDetails visitDetails, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, AppointmentNotFoundException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/appointment-service/secure/completeAppointment requestedBy={} role={} identifier={}", email, role, visitDetails.getAppointmentId());
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(appointmentService.completeAppointment(visitDetails));
    }

    @GetMapping("/getAppointment/{appointmentId}")
    public ResponseEntity<AppointmentDto> getAppointment(@PathVariable String appointmentId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, AppointmentNotFoundException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/appointment-service/secure/getAppointment/{} requestedBy={} role={}", appointmentId, email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(appointmentService.getAppointmentById(appointmentId));
    }

    @GetMapping("/getAllAppointments")
    public ResponseEntity<List<AppointmentDto>> getAllAppointments(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/appointment-service/secure/getAllAppointments requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(appointmentService.getAllAppointments());
    }

    @GetMapping("/getAppointmentsByUserId/{userId}")
    public ResponseEntity<List<AppointmentDto>> getAppointmentsByUserId(@PathVariable String userId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/appointment-service/secure/getAppointmentsByUserId/{} requestedBy={} role={}", userId, email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(appointmentService.getAppointmentsByUserId(userId));
    }

    @GetMapping("/getAppointmentsByDoctorId/{doctorId}")
    public ResponseEntity<List<AppointmentDto>> getAppointmentsByDoctorId(@PathVariable String doctorId, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/appointment-service/secure/getAppointmentsByDoctorId/{} requestedBy={} role={}", doctorId, email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(appointmentService.getAppointmentsByDoctorId(doctorId));
    }
}
