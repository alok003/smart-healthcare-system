package com.project.adminService.Service;

import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Exceptions.RequestNotFoundException;
import com.project.adminService.Model.RequestRoleDto;
import com.project.adminService.Repository.AdminRepository;
import com.project.adminService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class AdminService {
    private AdminRepository adminRepository;
    private UtilityFunctions utilityFunctions;

    public List<RequestRoleDto> getAllRequests(){
        List<RequestRole> requests=adminRepository.findAll();
        return requests
                .stream()
                .map(req->utilityFunctions.cnvEntityToBean(req))
                .toList();
    }

    public RequestRoleDto saveRequest(RequestRoleDto requestRoleDto){
        RequestRole requestRole=adminRepository.save(utilityFunctions.cnvBeanToEntity(requestRoleDto));
        return utilityFunctions.cnvEntityToBean(requestRole);
    }

    public String declineRequest(String id) throws RequestNotFoundException{
        if (!adminRepository.existsById(id)) {
            throw new RequestNotFoundException();
        }
        adminRepository.deleteById(id);
        return id;
    }

}
