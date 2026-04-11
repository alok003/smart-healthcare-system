package com.project.appointmentService.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointment-service/open")
public class MetricController {

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkConnection() {
        return ResponseEntity.status(HttpStatus.OK).body(Boolean.TRUE);
    }
}
