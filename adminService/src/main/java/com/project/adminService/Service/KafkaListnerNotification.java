package com.project.adminService.Service;

import com.project.adminService.Model.RequestRoleDto;
import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class KafkaListnerNotification {

    private AdminService adminService;

    @KafkaListener(topics= "role-request" , groupId = "admin-service-group")
    public void listenRoleRequest(RequestRoleDto requestRoleDto) {
        adminService.saveRequest(requestRoleDto);
    }
}
