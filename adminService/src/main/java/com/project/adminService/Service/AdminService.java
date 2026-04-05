package com.project.adminService.Service;

import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Exceptions.IllegalRequestException;
import com.project.adminService.Exceptions.RequestNotFoundException;
import com.project.adminService.Model.*;
import com.project.adminService.RESTCalls.DoctorClient;
import com.project.adminService.RESTCalls.PatientClient;
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
    private PatientClient patientClient;
    private ExternalServiceClient externalServiceClient;
    private static final String SYSTEM_EMAIL="system@shs.com";

    public List<RequestRoleDto> getAllActiveRequests() {
        List<RequestRole> requests = adminRepository.findByRequestStatus(Status.PENDING);
        return requests
                .stream()
                .map(req -> utilityFunctions.cnvEntityToBean(req))
                .toList();
    }

    public RequestRoleDto saveRequest(RequestRoleDto requestRoleDto) throws IllegalRequestException {
        if (requestRoleDto.getUserEmail() == null) throw new IllegalRequestException("userEmail is required");
        System.out.println("Saving role request for: " + requestRoleDto.getUserEmail() + ", role: " + requestRoleDto.getUserRole());
        Optional<RequestRole> requestRoleOptional = adminRepository.findByUserEmail(requestRoleDto.getUserEmail());
        RequestRole requestRole = null;
        if(requestRoleOptional.isEmpty()){
            requestRole = utilityFunctions.cnvBeanToEntity(requestRoleDto);
        }else{
            requestRole=requestRoleOptional.get();
            requestRole.setUserRole(requestRoleDto.getUserRole());
            requestRole.setDoctorDto(null);
            requestRole.setPatientDto(null);
            if(!requestRoleDto.getUserRole().name().equals("ADMIN")){
                if(requestRoleDto.getUserRole().name().equals("DOCTOR")){
                    requestRole.setDoctorDto(requestRoleDto.getDoctorDto());
                }
                else{
                    requestRole.setPatientDto(requestRoleDto.getPatientDto());
                }
            }

        }
        requestRole.setRequestStatus(Status.PENDING);
        RequestRoleDto saved = utilityFunctions.cnvEntityToBean(adminRepository.save(requestRole));
        System.out.println("Role request saved with id: " + saved.getId() + " for: " + requestRoleDto.getUserEmail());
        return saved;
    }

    public String declineRequest(String id) throws RequestNotFoundException, IllegalRequestException {
        Optional<RequestRole> requestRole = adminRepository.findById(id);
        if (requestRole.isEmpty()) {
            throw new RequestNotFoundException(id);
        } else if (requestRole.get().getRequestStatus().equals(Status.APPROVED)) {
            throw new IllegalRequestException("Cannot decline an already approved request: " + id);
        } else if (requestRole.get().getRequestStatus().equals(Status.DISCARDED)) {
            throw new IllegalRequestException("Request already discarded: " + id);
        } else {
            RequestRole request = requestRole.get();
            request.setRequestStatus(Status.DISCARDED);
            return adminRepository.save(request).getId();
        }
    }

    public String approveRequest(String id, String email, Integer maxCount, Double rate) throws RequestNotFoundException, IllegalRequestException {
        Optional<RequestRole> requestRole = adminRepository.findById(id);
        if (requestRole.isEmpty()) {
            throw new RequestNotFoundException(id);
        } else if (requestRole.get().getRequestStatus().equals(Status.APPROVED)) {
            throw new IllegalRequestException("Request already approved for id: " + id);
        } else if (requestRole.get().getRequestStatus().equals(Status.DISCARDED)) {
            throw new IllegalRequestException("Cannot approve a discarded request: " + id);
        } else {
            RequestRole request = requestRole.get();
            ChangeRequest changeRequest = ChangeRequest.builder()
                    .email(request.getUserEmail())
                    .role(request.getUserRole())
                    .build();
            ChangeRequest rollbackRequest = ChangeRequest.builder()
                    .email(request.getUserEmail())
                    .role(UserRole.USER)
                    .build();
            System.out.println("Approving request: " + id + " for: " + request.getUserEmail() + ", role: " + request.getUserRole());
            externalServiceClient.changeUserRole(changeRequest, email);
            System.out.println("Role changed to " + request.getUserRole() + " for: " + request.getUserEmail());
            try {
                if (request.getUserRole().name().equals("DOCTOR")) {
                    request.getDoctorDto().setEmail(request.getUserEmail());
                    externalServiceClient.saveDoctorProfile(request.getDoctorDto(), maxCount, rate, email);
                    System.out.println("Doctor profile saved for: " + request.getUserEmail());
                } else if (request.getUserRole().name().equals("PATIENT")) {
                    externalServiceClient.savePatientProfile(request.getPatientDto(), email);
                    System.out.println("Patient profile saved for: " + request.getUserEmail());
                }
            } catch (Exception e) {
                System.out.println("Failed to save doctor/patient for request: " + id + ", rolling back role change. Error: " + e.getMessage());
                userClient.changeRole(rollbackRequest, email, UserRole.ADMIN.name());
                throw new RuntimeException("Approval failed, role change reverted for: " + request.getUserEmail());
            }
            request.setRequestStatus(Status.APPROVED);
            String savedId = adminRepository.save(request).getId();
            System.out.println("Request approved successfully: " + savedId);
            return savedId;
        }
    }

    public Void approvePatientRequest() {
        List<RequestRole> requestRoles = adminRepository.findByUserRoleAndRequestStatus(UserRole.PATIENT, Status.PENDING);
        for (RequestRole request : requestRoles) {
            ChangeRequest changeRequest = ChangeRequest.builder()
                    .email(request.getUserEmail())
                    .role(request.getUserRole())
                    .build();
            ChangeRequest rollbackRequest = ChangeRequest.builder()
                    .email(request.getUserEmail())
                    .role(UserRole.USER)
                    .build();
            try {
                externalServiceClient.changeUserRole(changeRequest, SYSTEM_EMAIL);
                request.getPatientDto().setEmail(request.getUserEmail());
                externalServiceClient.savePatientProfile(request.getPatientDto(), SYSTEM_EMAIL);
                request.setRequestStatus(Status.APPROVED);
                adminRepository.save(request);
            } catch (Exception e) {
                System.out.println("Failed to approve patient request for: " + request.getUserEmail() + ", rolling back. Error: " + e.getMessage());
                userClient.changeRole(rollbackRequest, SYSTEM_EMAIL, UserRole.ADMIN.name());
            }
        }
        return null;
    }

    public RequestRoleDto checkStatus(String email) throws RequestNotFoundException {
        Optional<RequestRole> requestRole = adminRepository.findByUserEmail(email);
        if (requestRole.isEmpty()) {
            throw new RequestNotFoundException(email);
        }
        return utilityFunctions.cnvEntityToBean(requestRole.get());
    }
}
