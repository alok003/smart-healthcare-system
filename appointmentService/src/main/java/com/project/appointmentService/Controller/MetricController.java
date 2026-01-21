package com.project.appointmentService.Controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin-service/open")
@AllArgsConstructor
public class MetricController {

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkConnection() {
        return ResponseEntity.ok(Boolean.TRUE);
    }
}
