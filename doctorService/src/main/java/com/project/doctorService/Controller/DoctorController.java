package com.project.doctorService.Controller;

import com.project.doctorService.Exceptions.UnAuthorizedException;
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
    public ResponseEntity<DoctorDto> saveRequest(@RequestBody DoctorDto doctorDto, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(doctorService.saveRequest(doctorDto));
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

}
