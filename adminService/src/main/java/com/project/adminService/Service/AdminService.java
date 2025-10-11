package com.project.adminService.Service;

import com.project.adminService.Entity.Doctor;
import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Exceptions.RequestNotFoundException;
import com.project.adminService.Model.*;
import com.project.adminService.RESTCalls.DoctorClient;
import com.project.adminService.RESTCalls.UserClient;
import com.project.adminService.Repository.AdminRepository;
import com.project.adminService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AdminService {
    private AdminRepository adminRepository;
    private UtilityFunctions utilityFunctions;
    private UserClient userClient;
    private DoctorClient doctorClient;

    public List<RequestRoleDto> getAllRequests() {
        List<RequestRole> requests = adminRepository.findAll();
        return requests
                .stream()
                .map(req -> utilityFunctions.cnvEntityToBean(req))
                .toList();
    }

    public RequestRoleDto saveRequest(RequestRoleDto requestRoleDto) {
        RequestRole requestRole = utilityFunctions.cnvBeanToEntity(requestRoleDto);//chk hre
        requestRole.setRequestStatus(Status.PENDING);
        return utilityFunctions.cnvEntityToBean(adminRepository.save(requestRole));
    }

    public String declineRequest(String id) throws RequestNotFoundException {
        Optional<RequestRole> requestRole = adminRepository.findById(id);
        if (requestRole.isEmpty()) {
            throw new RequestNotFoundException();
        } else {
            RequestRole request = requestRole.get();
            request.setRequestStatus(Status.DISCARDED);
            return adminRepository.save(request).getId();
        }
    }

    public String approveRequest(String id, String email) throws RequestNotFoundException {
        Optional<RequestRole> requestRole = adminRepository.findById(id);
        if (requestRole.isEmpty()) {
            throw new RequestNotFoundException();
        } else {
            RequestRole request = requestRole.get();
            ChangeRequest changeRequest = ChangeRequest.builder()
                    .email(request.getUserEmail())
                    .role(request.getUserRole())
                    .build();

            String responseUserId = userClient.changeRole(changeRequest, email, UserRole.ADMIN.name());
            Doctor doctor = request.getDoctor();
            if (!request.getUserRole().name().equals("ADMIN")) {
                String responseDoctorId = doctorClient.saveDoctor(utilityFunctions.cnvEntityToBeanDoctor(doctor), email, UserRole.ADMIN.name());
            }
            request.setRequestStatus(Status.APPROVED);
            return adminRepository.save(request).getId();
        }
    }
}
