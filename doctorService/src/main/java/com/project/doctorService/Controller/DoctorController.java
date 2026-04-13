package com.project.doctorService.Controller;

import com.project.doctorService.Exceptions.DateOutOfRangeException;
import com.project.doctorService.Exceptions.DoctorNotFoundException;
import com.project.doctorService.Exceptions.UnAuthorizedException;
import com.project.doctorService.Model.AppointmentDto;
import com.project.doctorService.Model.DoctorDto;
import com.project.doctorService.Model.VisitDetails;
import com.project.doctorService.Service.DoctorService;
import com.project.doctorService.Utility.UtilityFunctions;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctor-service/secure")
@AllArgsConstructor
public class DoctorController {

    private static final Logger log = LoggerFactory.getLogger(DoctorController.class);

    private DoctorService doctorService;
    private UtilityFunctions utilityFunctions;

    @PostMapping("/saveDoctor")
    public ResponseEntity<DoctorDto> saveRequest(@RequestBody DoctorDto doctorDto, @RequestParam int maxCount, @RequestParam double rate, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/doctor-service/secure/saveDoctor requestedBy={} role={} identifier={}", email, role, doctorDto.getEmail());
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.saveRequest(doctorDto, maxCount, rate));
    }

    @GetMapping("/getMyDetails")
    public ResponseEntity<DoctorDto> getMyDetails(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, DoctorNotFoundException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/doctor-service/secure/getMyDetails requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestDoctor(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(doctorService.getMyDetails(email));
    }

    @GetMapping("/getMyAppointments")
    public ResponseEntity<List<AppointmentDto>> getMyAppointments(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/doctor-service/secure/getMyAppointments requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestDoctor(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(doctorService.getMyAppointments(email));
    }

    @GetMapping("/getAllDoctors")
    public ResponseEntity<List<DoctorDto>> getAllDoctors(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/doctor-service/secure/getAllDoctors requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(doctorService.getAllDoctors());
    }

    @PostMapping("/addLeave")
    public ResponseEntity<DoctorDto> addLeave(@RequestBody List<LocalDate> leave, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, DoctorNotFoundException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/doctor-service/secure/addLeave requestedBy={} role={} days={}", email, role, leave.size());
        if (!utilityFunctions.validateRequestDoctor(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(doctorService.addLeave(email, leave));
    }

    @GetMapping("/getDoctorByEmail/{doctorEmail}")
    public ResponseEntity<DoctorDto> getDoctorByEmail(@PathVariable String doctorEmail, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, DoctorNotFoundException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/doctor-service/secure/getDoctorByEmail/{} requestedBy={} role={}", doctorEmail, email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(doctorService.getMyDetails(doctorEmail));
    }

    @DeleteMapping("/removeAppointmentFromSchedule")
    public ResponseEntity<Void> removeAppointmentFromSchedule(@RequestParam String appointmentId, @RequestParam String date, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=DELETE path=/api/doctor-service/secure/removeAppointmentFromSchedule requestedBy={} role={} identifier={}", email, role, appointmentId);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        doctorService.removeAppointmentFromSchedule(appointmentId, email, date);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/addDocAppointment")
    public ResponseEntity<Boolean> addDocAppointment(@RequestBody AppointmentDto appointmentDto, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, DoctorNotFoundException, DateOutOfRangeException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/doctor-service/secure/addDocAppointment requestedBy={} role={} identifier={}", email, role, appointmentDto.getId());
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(doctorService.addDocAppointment(appointmentDto, email));
    }

    @PostMapping("/completeAppointment")
    public ResponseEntity<AppointmentDto> completeAppointment(@Valid @RequestBody VisitDetails visitDetails, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, DoctorNotFoundException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/doctor-service/secure/completeAppointment requestedBy={} role={} identifier={}", email, role, visitDetails.getAppointmentId());
        if (!utilityFunctions.validateRequestDoctor(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(doctorService.completeAppointment(visitDetails, email));
    }
}
