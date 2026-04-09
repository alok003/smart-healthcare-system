package com.project.adminService.Service;

import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Exceptions.IllegalRequestException;
import com.project.adminService.Exceptions.RequestNotFoundException;
import com.project.adminService.Model.*;
import com.project.adminService.Repository.AdminRepository;
import com.project.adminService.Utility.LogUtil;
import com.project.adminService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private AdminRepository adminRepository;
    private UtilityFunctions utilityFunctions;
    private ExternalServiceClient externalServiceClient;
    private static final String SYSTEM_EMAIL = "system@shs.com";

    public List<RequestRoleDto> getAllActiveRequests() {
        List<RequestRole> requests = adminRepository.findByRequestStatus(Status.PENDING);
        log.debug("action=FETCH_REQUESTS status=SUCCESS entity=RequestRole filter=PENDING");
        return requests.stream().map(utilityFunctions::cnvEntityToBean).toList();
    }

    public RequestRoleDto saveRequest(RequestRoleDto requestRoleDto) throws IllegalRequestException {
        if (requestRoleDto.getUserEmail() == null) throw new IllegalRequestException("userEmail is required");
        log.info("action=SAVE_REQUEST status=INITIATED identifier={} targetRole={} payload={}", requestRoleDto.getUserEmail(), requestRoleDto.getUserRole(), LogUtil.toJson(requestRoleDto));
        Optional<RequestRole> existing = adminRepository.findByUserEmail(requestRoleDto.getUserEmail());
        RequestRole requestRole;
        if (existing.isEmpty()) {
            log.debug("action=SAVE_REQUEST detail=NEW_REQUEST identifier={}", requestRoleDto.getUserEmail());
            requestRole = utilityFunctions.cnvBeanToEntity(requestRoleDto);
        } else {
            log.debug("action=SAVE_REQUEST detail=UPDATING_EXISTING_REQUEST identifier={}", requestRoleDto.getUserEmail());
            requestRole = existing.get();
            requestRole.setUserRole(requestRoleDto.getUserRole());
            requestRole.setDoctorDto(null);
            requestRole.setPatientDto(null);
            if (!requestRoleDto.getUserRole().equals(UserRole.ADMIN)) {
                if (requestRoleDto.getUserRole().equals(UserRole.DOCTOR)) {
                    requestRole.setDoctorDto(requestRoleDto.getDoctorDto());
                } else {
                    requestRole.setPatientDto(requestRoleDto.getPatientDto());
                }
            }
        }
        requestRole.setRequestStatus(Status.PENDING);
        RequestRoleDto saved = utilityFunctions.cnvEntityToBean(adminRepository.save(requestRole));
        log.info("action=SAVE_REQUEST status=SUCCESS identifier={} id={} targetRole={} payload={}", requestRoleDto.getUserEmail(), saved.getId(), requestRoleDto.getUserRole(), LogUtil.toJson(saved));
        return saved;
    }

    public String declineRequest(String id) throws RequestNotFoundException, IllegalRequestException {
        log.info("action=DECLINE_REQUEST status=INITIATED id={}", id);
        Optional<RequestRole> requestRole = adminRepository.findById(id);
        if (requestRole.isEmpty()) {
            throw new RequestNotFoundException(id);
        } else if (requestRole.get().getRequestStatus().equals(Status.APPROVED)) {
            throw new IllegalRequestException("Cannot decline an already approved request: " + id);
        } else if (requestRole.get().getRequestStatus().equals(Status.DISCARDED)) {
            throw new IllegalRequestException("Request already discarded: " + id);
        }
        RequestRole request = requestRole.get();
        request.setRequestStatus(Status.DISCARDED);
        String savedId = adminRepository.save(request).getId();
        log.info("action=DECLINE_REQUEST status=SUCCESS id={} identifier={}", savedId, request.getUserEmail());
        return savedId;
    }

    public String approveRequest(String id, String email, Integer maxCount, Double rate) throws RequestNotFoundException, IllegalRequestException {
        log.info("action=APPROVE_REQUEST status=INITIATED id={} requestedBy={}", id, email);
        Optional<RequestRole> requestRole = adminRepository.findById(id);
        if (requestRole.isEmpty()) {
            throw new RequestNotFoundException(id);
        } else if (requestRole.get().getRequestStatus().equals(Status.APPROVED)) {
            throw new IllegalRequestException("Request already approved for id: " + id);
        } else if (requestRole.get().getRequestStatus().equals(Status.DISCARDED)) {
            throw new IllegalRequestException("Cannot approve a discarded request: " + id);
        }
        RequestRole request = requestRole.get();
        ChangeRequest changeRequest = ChangeRequest.builder().email(request.getUserEmail()).role(request.getUserRole()).build();
        ChangeRequest rollbackRequest = ChangeRequest.builder().email(request.getUserEmail()).role(UserRole.USER).build();

        externalServiceClient.changeUserRole(changeRequest, email);
        log.info("action=ROLE_CHANGE status=SUCCESS identifier={} targetRole={} requestedBy={}", request.getUserEmail(), request.getUserRole(), email);

        try {
            if (request.getUserRole().equals(UserRole.DOCTOR)) {
                request.getDoctorDto().setEmail(request.getUserEmail());
                externalServiceClient.saveDoctorProfile(request.getDoctorDto(), maxCount, rate, email);
            } else if (request.getUserRole().equals(UserRole.PATIENT)) {
                externalServiceClient.savePatientProfile(request.getPatientDto(), email);
            }
        } catch (Exception e) {
            log.error("action=APPROVE_REQUEST status=FAILED id={} identifier={} requestedBy={} reason=PROFILE_SAVE_FAILED detail=Role change reverted to USER error={}", id, request.getUserEmail(), email, e.getMessage());
            externalServiceClient.changeUserRole(rollbackRequest, email);
            throw new RuntimeException("Approval failed, role change reverted for: " + request.getUserEmail());
        }

        request.setRequestStatus(Status.APPROVED);
        String savedId = adminRepository.save(request).getId();
        log.info("action=APPROVE_REQUEST status=SUCCESS id={} identifier={} role={} requestedBy={}", savedId, request.getUserEmail(), request.getUserRole(), email);
        return savedId;
    }

    public void approvePatientRequest() {
        List<RequestRole> requestRoles = adminRepository.findByUserRoleAndRequestStatus(UserRole.PATIENT, Status.PENDING);
        log.info("action=BULK_APPROVE_PATIENTS status=INITIATED count={} requestedBy={}", requestRoles.size(), SYSTEM_EMAIL);
        for (RequestRole request : requestRoles) {
            ChangeRequest changeRequest = ChangeRequest.builder().email(request.getUserEmail()).role(request.getUserRole()).build();
            ChangeRequest rollbackRequest = ChangeRequest.builder().email(request.getUserEmail()).role(UserRole.USER).build();
            try {
                externalServiceClient.changeUserRole(changeRequest, SYSTEM_EMAIL);
                request.getPatientDto().setEmail(request.getUserEmail());
                externalServiceClient.savePatientProfile(request.getPatientDto(), SYSTEM_EMAIL);
                request.setRequestStatus(Status.APPROVED);
                adminRepository.save(request);
                log.info("action=BULK_APPROVE_PATIENTS status=SUCCESS identifier={}", request.getUserEmail());
            } catch (Exception e) {
                log.error("action=BULK_APPROVE_PATIENTS status=FAILED identifier={} reason=PROFILE_SAVE_FAILED detail=Role change reverted error={}", request.getUserEmail(), e.getMessage());
                externalServiceClient.changeUserRole(rollbackRequest, SYSTEM_EMAIL);
            }
        }
    }

    public RequestRoleDto checkStatus(String email) throws RequestNotFoundException {
        log.info("action=CHECK_STATUS status=INITIATED identifier={}", email);
        Optional<RequestRole> requestRole = adminRepository.findByUserEmail(email);
        if (requestRole.isEmpty()) throw new RequestNotFoundException(email);
        RequestRoleDto result = utilityFunctions.cnvEntityToBean(requestRole.get());
        log.info("action=CHECK_STATUS status=SUCCESS identifier={} payload={}", email, LogUtil.toJson(result));
        return result;
    }
}
