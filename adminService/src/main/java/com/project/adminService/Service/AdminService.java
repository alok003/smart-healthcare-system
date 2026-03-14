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
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AdminService {
    private AdminRepository adminRepository;
    private UtilityFunctions utilityFunctions;
    private UserClient userClient;
    private DoctorClient doctorClient;
    private PatientClient patientClient;
    private static final String SYSTEM_EMAIL="system@shs.com";

    public List<RequestRoleDto> getAllActiveRequests() {
        List<RequestRole> requests = adminRepository.findByRequestStatus(Status.PENDING);
        return requests
                .stream()
                .map(req -> utilityFunctions.cnvEntityToBean(req))
                .toList();
    }

    public RequestRoleDto saveRequest(RequestRoleDto requestRoleDto) throws IllegalRequestException {
        Optional<RequestRole> requestRoleOptional = adminRepository.findByUserEmail(requestRoleDto.getUserEmail());
        RequestRole requestRole = null;
        if(requestRoleOptional.isEmpty()){
            requestRole = utilityFunctions.cnvBeanToEntity(requestRoleDto);
        }else{
            requestRole=requestRoleOptional.get();
            requestRole.setUserRole(requestRoleDto.getUserRole());
            requestRole.setDoctorDto(null);
            requestRole.setPatientDto(null);
            if(requestRoleDto.getUserRole().name().equals("DOCTOR")&&Objects.nonNull(requestRoleDto.getDoctorDto())){
                requestRole.setDoctorDto(requestRoleDto.getDoctorDto());
            }
            else if(requestRoleDto.getUserRole().name().equals("PATIENT")&&Objects.nonNull(requestRoleDto.getPatientDto())){
                requestRole.setPatientDto(requestRoleDto.getPatientDto());
            }
            else{
                throw new IllegalRequestException();
            }
        }
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

    public String approveRequest(String id, String email,int maxCount, double rate) throws RequestNotFoundException {
        Optional<RequestRole> requestRole = adminRepository.findById(id);
        if (requestRole.isEmpty()||requestRole.get().getRequestStatus().equals(Status.APPROVED)) {
            throw new RequestNotFoundException();
        } else {
            RequestRole request = requestRole.get();
            ChangeRequest changeRequest = ChangeRequest.builder()
                    .email(request.getUserEmail())
                    .role(request.getUserRole())
                    .build();

            String responseUserId = userClient.changeRole(changeRequest, email, UserRole.ADMIN.name());

            if (request.getUserRole().name().equals("DOCTOR")) {
                DoctorDto doctorDto=request.getDoctorDto();
                String responseDoctorId = doctorClient.saveDoctor(doctorDto, maxCount, rate, email, UserRole.ADMIN.name());
            }else if(request.getUserRole().name().equals("PATIENT")){
                PatientDto patientDto=request.getPatientDto();
                String responsePatientId = patientClient.savePatient(patientDto, email, UserRole.ADMIN.name());
            }
            request.setRequestStatus(Status.APPROVED);
            return adminRepository.save(request).getId();
        }
    }

    public Void approvePatientRequest() throws RequestNotFoundException {
            List<RequestRole> requestRoles = adminRepository.findByUserRoleAndRequestStatus(UserRole.PATIENT, Status.PENDING);
            for(RequestRole request:requestRoles){
                ChangeRequest changeRequest = ChangeRequest.builder()
                        .email(request.getUserEmail())
                        .role(request.getUserRole())
                        .build();

                String responseUserId = userClient.changeRole(changeRequest, SYSTEM_EMAIL, UserRole.ADMIN.name());
                PatientDto patientDto=request.getPatientDto();
                String responsePatientId = patientClient.savePatient(patientDto, SYSTEM_EMAIL, UserRole.ADMIN.name());
                request.setRequestStatus(Status.APPROVED);
                adminRepository.save(request);
            }
            return null;
    }

    public RequestRoleDto checkStatus(String email) throws RequestNotFoundException {
        Optional<RequestRole> requestRole = adminRepository.findByUserEmail(email);
        if (requestRole.isEmpty()) {
            throw new RequestNotFoundException();
        }
        return utilityFunctions.cnvEntityToBean(requestRole.get());
    }
}
