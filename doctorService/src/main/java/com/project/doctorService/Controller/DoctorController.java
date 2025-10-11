package com.project.doctorService.Controller;

import com.project.doctorService.Exceptions.UnAuthorizedException;
import com.project.doctorService.Model.DoctorDto;
import com.project.doctorService.Service.DoctorService;
import com.project.doctorService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor-service/secure")
@AllArgsConstructor
public class DoctorController {
    private DoctorService doctorService;
    private UtilityFunctions utilityFunctions;

    @PostMapping("/saveDoctor")
    public ResponseEntity<DoctorDto> getAllRequests(@RequestBody DoctorDto doctorDto, @RequestHeader("X-User-Email") String email, @RequestHeader("X-User-Role") String role) throws UnAuthorizedException {
        if(!utilityFunctions.validateRequestAdmin(email, role)) throw new UnAuthorizedException();
        return ResponseEntity.ok(doctorService.saveRequest(doctorDto));
    }

}
