package com.project.patientService.Service;

import com.project.patientService.Utility.LogUtil;
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
public class KafkaListenerPatient {

    private static final Logger log = LoggerFactory.getLogger(KafkaListenerPatient.class);

    private PatientService patientService;

    @KafkaListener(topics = "appointment-completed", groupId = "patient-service-group")
    public void handleAppointmentCompleted(@Payload Map<String, Object> message,
                                           @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            String appointmentId = (String) message.get("id");
            String patientId = (String) message.get("patientId");
            String dateStr = (String) message.get("date");
            Object visitDetailsObj = message.get("visitDetails");
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=appointment-completed identifier={} patientId={}", appointmentId, patientId);
            if (patientId == null || dateStr == null || visitDetailsObj == null) {
                log.warn("action=KAFKA_CONSUME status=SKIPPED topic=appointment-completed identifier={} reason=MISSING_FIELDS", appointmentId);
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> healthCheckMap = (Map<String, Object>) ((Map<String, Object>) visitDetailsObj).get("healthCheck");
            if (healthCheckMap == null) {
                log.warn("action=KAFKA_CONSUME status=SKIPPED topic=appointment-completed identifier={} reason=HEALTH_CHECK_NULL", appointmentId);
                return;
            }
            patientService.updatePatientVitals(appointmentId, patientId, dateStr, healthCheckMap);
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=appointment-completed identifier={} patientId={}", appointmentId, patientId);
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=appointment-completed error={}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }
}
