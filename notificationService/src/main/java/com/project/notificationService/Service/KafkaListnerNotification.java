package com.project.notificationService.Service;

import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class KafkaListnerNotification {

    private EmailService emailService;

    @KafkaListener(topics = "welcome-notification", groupId = "notification-service-group")
    public void listenWelcomeNotification(Map<String,Object> message) {
        emailService.sendWelcomeEmail(message);
    }

    @KafkaListener(topics = "send-email-appointment", groupId = "notification-service-group")
    public void listenAppointmentNotification(Map<String,Object> message) {
        emailService.sendAppointmentEmail(message);
    }
}
