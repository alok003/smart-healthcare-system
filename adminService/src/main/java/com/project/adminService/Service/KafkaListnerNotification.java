package com.project.adminService.Service;

import com.project.adminService.Model.RequestRoleDto;
import com.project.adminService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class KafkaListnerNotification {

    private AdminService adminService;
    private UtilityFunctions utilityFunctions;

    @KafkaListener(topics= "role-request" , groupId = "admin-service-group")
    public void listenRoleRequest(Map<String,Object> message) {
        adminService.saveRequest(UtilityFunctions.cnvMapToDto(message, RequestRoleDto.class));
    }
}
