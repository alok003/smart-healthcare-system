package com.project.doctorService.Service;

import com.project.doctorService.Utility.LogUtil;
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
public class KafkaListenerDoctor {

    private static final Logger log = LoggerFactory.getLogger(KafkaListenerDoctor.class);

    private DoctorService doctorService;

    @KafkaListener(topics = "appointment-cancelled", groupId = "doctor-service-group")
    public void handleAppointmentCancelled(@Payload Map<String, Object> message,
                                           @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            String appointmentId = (String) message.get("id");
            String doctorId = (String) message.get("doctorId");
            String dateStr = (String) message.get("date");
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=appointment-cancelled identifier={} doctorId={}", appointmentId, doctorId);
            if (appointmentId == null || doctorId == null || dateStr == null) {
                log.warn("action=KAFKA_CONSUME status=SKIPPED topic=appointment-cancelled reason=MISSING_FIELDS payload={}", LogUtil.toJson(message));
                return;
            }
            doctorService.removeAppointmentFromSchedule(appointmentId, doctorId, dateStr);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=appointment-cancelled identifier={} doctorId={}", appointmentId, doctorId);
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=appointment-cancelled error={}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }
}
