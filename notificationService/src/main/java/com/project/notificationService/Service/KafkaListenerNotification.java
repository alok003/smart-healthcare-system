package com.project.notificationService.Service;

import com.project.notificationService.Utility.LogUtil;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class KafkaListenerNotification {

    private static final Logger log = LoggerFactory.getLogger(KafkaListenerNotification.class);

    private EmailService emailService;

    @KafkaListener(topics = "welcome-notification", groupId = "notification-service-group")
    public void listenWelcomeNotification(@Payload Map<String, Object> message,
                                          @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=welcome-notification identifier={}", message.get("userEmail"));
            emailService.sendWelcomeEmail(message);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=welcome-notification identifier={}", message.get("userEmail"));
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=welcome-notification identifier={} error={}", message.get("userEmail"), e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "role-approved", groupId = "notification-service-group")
    public void listenRoleApproved(@Payload Map<String, Object> message,
                                   @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=role-approved identifier={}", message.get("userEmail"));
            emailService.sendRoleApprovedEmail(message);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=role-approved identifier={}", message.get("userEmail"));
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=role-approved identifier={} error={}", message.get("userEmail"), e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "role-declined", groupId = "notification-service-group")
    public void listenRoleDeclined(@Payload Map<String, Object> message,
                                   @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=role-declined identifier={}", message.get("userEmail"));
            emailService.sendRoleDeclinedEmail(message);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=role-declined identifier={}", message.get("userEmail"));
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=role-declined identifier={} error={}", message.get("userEmail"), e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "appointment-booked", groupId = "notification-service-group")
    public void listenAppointmentBooked(@Payload Map<String, Object> message,
                                        @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=appointment-booked identifier={} patientId={} doctorId={}", message.get("id"), message.get("patientId"), message.get("doctorId"));
            emailService.sendAppointmentBookedEmail(message);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=appointment-booked identifier={}", message.get("id"));
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=appointment-booked identifier={} error={}", message.get("id"), e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "appointment-cancelled-notification", groupId = "notification-service-group")
    public void listenAppointmentCancelled(@Payload Map<String, Object> message,
                                           @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=appointment-cancelled-notification identifier={} patientId={}", message.get("id"), message.get("patientId"));
            emailService.sendAppointmentCancelledEmail(message);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=appointment-cancelled-notification identifier={}", message.get("id"));
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=appointment-cancelled-notification identifier={} error={}", message.get("id"), e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "appointment-completed", groupId = "notification-service-group")
    public void listenAppointmentCompleted(@Payload Map<String, Object> message,
                                           @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=appointment-completed identifier={} patientId={}", message.get("id"), message.get("patientId"));
            emailService.sendAppointmentCompletedEmail(message);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=appointment-completed identifier={}", message.get("id"));
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=appointment-completed identifier={} error={}", message.get("id"), e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "send-email-appointment", groupId = "notification-service-group")
    public void listenPrescriptionReady(@Payload Map<String, Object> message,
                                        @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=send-email-appointment identifier={} patientId={}", message.get("id"), message.get("patientId"));
            emailService.sendPrescriptionEmail(message);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=send-email-appointment identifier={}", message.get("id"));
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=send-email-appointment identifier={} error={}", message.get("id"), e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "doctor-daily-schedule", groupId = "notification-service-group")
    public void listenDoctorDailySchedule(@Payload Map<String, Object> message,
                                          @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=doctor-daily-schedule doctorEmail={} date={}", message.get("doctorEmail"), message.get("date"));
            emailService.sendDoctorDailyScheduleEmail(message);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=doctor-daily-schedule doctorEmail={}", message.get("doctorEmail"));
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=doctor-daily-schedule doctorEmail={} error={}", message.get("doctorEmail"), e.getMessage());
        } finally {
            MDC.clear();
        }
    }
}
