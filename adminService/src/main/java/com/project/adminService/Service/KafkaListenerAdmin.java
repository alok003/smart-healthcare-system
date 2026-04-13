package com.project.adminService.Service;

import com.project.adminService.Model.RequestRoleDto;
import com.project.adminService.Utility.LogUtil;
import com.project.adminService.Utility.UtilityFunctions;
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
public class KafkaListenerAdmin {

    private static final Logger log = LoggerFactory.getLogger(KafkaListenerAdmin.class);

    private AdminService adminService;

    @KafkaListener(topics = "role-request", groupId = "admin-service-group")
    public void listenRoleRequest(@Payload Map<String, Object> message,
                                  @Header(value = "X-Correlation-ID", required = false) String corrId) {
        MDC.put("correlationId", corrId != null ? corrId : "KAFKA-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            log.info("action=KAFKA_CONSUME status=RECEIVED topic=role-request payload={}", LogUtil.toJson(message));
            adminService.saveRequest(UtilityFunctions.cnvMapToDto(message, RequestRoleDto.class));
            log.info("action=KAFKA_CONSUME status=PROCESSED topic=role-request identifier={}", message.get("userEmail"));
        } catch (Exception e) {
            log.error("action=KAFKA_CONSUME status=FAILED topic=role-request identifier={} reason=SAVE_REQUEST_FAILED error={}", message.get("userEmail"), e.getMessage());
        } finally {
            MDC.clear();
        }
    }
}
