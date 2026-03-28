package com.project.doctorService.Controller;

import com.project.doctorService.Exceptions.UnAuthorizedException;
import com.project.doctorService.Model.AppointmentDto;
import com.project.doctorService.Model.DoctorDto;
import com.project.doctorService.Service.DoctorService;
import com.project.doctorService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctor-service/secure")
@AllArgsConstructor
public class DoctorController {
    private DoctorService doctorService;
    private UtilityFunctions utilityFunctions;

    @PostMapping("/saveDoctor")
    public ResponseEntity<DoctorDto> saveRequest(@RequestBody DoctorDto doctorDto, @RequestParam int maxCount, @RequestParam double rate, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(doctorService.saveRequest(doctorDto,maxCount,rate));
    }

    @GetMapping("/getMyDetails")
    public ResponseEntity<DoctorDto> getMyDetails(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestDoctor(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(doctorService.getMyDetails(email));
    }

    @GetMapping("/getAllDoctors")
    public ResponseEntity<List<DoctorDto>> getAllDoctors(@RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @PostMapping("/addLeave")
    public ResponseEntity<DoctorDto> addLeave(@RequestBody List<LocalDate> leave, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestDoctor(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(doctorService.addLeave(email,leave));
    }

    @PostMapping("/addDocAppointment")
    public ResponseEntity<Boolean> addDocAppointment(@RequestBody AppointmentDto appointmentDto, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(doctorService.addDocAppointment(appointmentDto,email));
    }

}
