package com.project.patientService.Controller;

import com.project.patientService.Exceptions.PatientNotFoundException;
import com.project.patientService.Exceptions.UnAuthorizedException;
import com.project.patientService.Model.AppointmentDto;
import com.project.patientService.Model.DoctorDto;
import com.project.patientService.Model.PatientDto;
import com.project.patientService.Service.PatientService;
import com.project.patientService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/patient-service/secure")
public class PatientController {

    private static final Logger log = LoggerFactory.getLogger(PatientController.class);

    private UtilityFunctions utilityFunctions;
    private PatientService patientService;

    @PostMapping("/savePatient")
    public ResponseEntity<PatientDto> savePatient(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @RequestBody PatientDto patientDto) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/patient-service/secure/savePatient requestedBy={} role={} identifier={}", email, role, patientDto.getEmail());
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.savePatient(patientDto));
    }

    @GetMapping("/getMyProfile")
    public ResponseEntity<PatientDto> getMyProfile(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException, PatientNotFoundException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/patient-service/secure/getMyProfile requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(patientService.getMyProfile(email));
    }

    @PostMapping("/bookAppointment")
    public ResponseEntity<AppointmentDto> bookAppointment(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @RequestBody AppointmentDto appointmentDto) throws UnAuthorizedException, PatientNotFoundException {
        log.info("action=REQUEST_RECEIVED method=POST path=/api/patient-service/secure/bookAppointment requestedBy={} role={} doctorId={} date={}", email, role, appointmentDto.getDoctorId(), appointmentDto.getDate());
        if (!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.bookAppointment(appointmentDto, email));
    }

    @GetMapping("/getDocAvailability")
    public ResponseEntity<List<DoctorDto>> getDoctorAvailability(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/patient-service/secure/getDocAvailability requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(patientService.getDoctorAvailibility(email));
    }

    @DeleteMapping("/removeAppointment/{appointmentId}")
    public ResponseEntity<Void> removeAppointment(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @PathVariable String appointmentId) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=DELETE path=/api/patient-service/secure/removeAppointment/{} requestedBy={} role={}", appointmentId, email, role);
        if (!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        patientService.removeAppointmentFromPatientByAppointmentId(appointmentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/cancelAppointment/{id}")
    public ResponseEntity<AppointmentDto> cancelAppointment(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @PathVariable String id) throws UnAuthorizedException, PatientNotFoundException {
        log.info("action=REQUEST_RECEIVED method=DELETE path=/api/patient-service/secure/cancelAppointment/{} requestedBy={} role={}", id, email, role);
        if (!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(patientService.cancelAppointment(id, email));
    }

    @GetMapping("/getAllAppointments")
    public ResponseEntity<List<AppointmentDto>> getAllAppointmentsPatient(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/patient-service/secure/getAllAppointments requestedBy={} role={}", email, role);
        if (!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.OK).body(patientService.getAllAppointmentsbyPatient(email));
    }

    @GetMapping("/getPrescription/{id}")
    public ResponseEntity<String> getPrescription(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @PathVariable String id) throws UnAuthorizedException, PatientNotFoundException {
        log.info("action=REQUEST_RECEIVED method=GET path=/api/patient-service/secure/getPrescription/{} requestedBy={} role={}", id, email, role);
        if (!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(patientService.getPrescription(id, email));
    }
}
