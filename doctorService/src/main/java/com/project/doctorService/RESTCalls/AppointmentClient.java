package com.project.doctorService.RESTCalls;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="appointment-service")
public interface AppointmentClient {

    @PostMapping("/api/appointment-service/secure/cancelAppointments")
    Boolean cancelAppointmentAppointmentClient(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @RequestBody String AppointmentId
    );

}
