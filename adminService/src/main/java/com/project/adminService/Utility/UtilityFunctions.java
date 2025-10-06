package com.project.adminService.Utility;

import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Model.RequestRoleDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UtilityFunctions {
    public RequestRole cnvBeanToEntity(RequestRoleDto requestRoleDto) {
        RequestRole requestRole = new RequestRole();
        BeanUtils.copyProperties(requestRoleDto, requestRole);
        return requestRole;
    }

    public RequestRoleDto cnvEntityToBean(RequestRole requestRole) {
        RequestRoleDto requestRoleDto= new RequestRoleDto();
        BeanUtils.copyProperties(requestRole, requestRoleDto);
        return requestRoleDto;
    }

    public Boolean validateRequestAdmin(String email,String role){
        return Objects.equals(role, "ADMIN")&&validateEmail(email);
    }

    private boolean validateEmail(String email) {
        return email != null && email.contains("@");
    }
}
