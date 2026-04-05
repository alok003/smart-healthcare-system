package com.project.patientService.Controller;

import com.project.patientService.Exception.PatientNotFoundException;
import com.project.patientService.Exception.UnAuthorizedException;
import com.project.patientService.Model.AppointmentDto;
import com.project.patientService.Model.DoctorDto;
import com.project.patientService.Model.PatientDto;
import com.project.patientService.Service.PatientService;
import com.project.patientService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/patient-service/secure")
public class PatientController {

    private UtilityFunctions utilityFunctions;
    private PatientService patientService;

    @PostMapping("/savePatient")
    public ResponseEntity<PatientDto> savePatient(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @RequestBody PatientDto patientDto) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(patientService.savePatient(patientDto));
    }

    @PostMapping("/bookAppointment")
    public ResponseEntity<AppointmentDto> bookAppointment(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @RequestBody AppointmentDto appointmentDto) throws UnAuthorizedException, PatientNotFoundException {
        if(!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(patientService.bookAppointment(appointmentDto,email));
    }

    @GetMapping("/getDocAvailability")
    public ResponseEntity<List<DoctorDto>> getDoctorAvailibility(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(patientService.getDoctorAvailibility(email));
    }

    @DeleteMapping("/cancelAppointment/{id}")
    public ResponseEntity<AppointmentDto> cancelAppointment(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @PathVariable String id) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(patientService.cancelAppointment(id,email));
    }

    @GetMapping("/getAllAppointments")
    public ResponseEntity<List<AppointmentDto>> getAllAppointmentsPatient(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(patientService.getAllAppointmentsbyPatient(email));
    }

    @GetMapping("getPrescription/{id}")
    public ResponseEntity<String> getPrescription(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role, @PathVariable String id) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestPatient(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(patientService.getPrescription(id,email));
    }
}
