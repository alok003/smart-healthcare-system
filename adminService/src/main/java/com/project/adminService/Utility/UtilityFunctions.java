package com.project.adminService.Utility;

import com.project.adminService.Entity.Doctor;
import com.project.adminService.Entity.RequestRole;
import com.project.adminService.Model.DoctorDto;
import com.project.adminService.Model.RequestRoleDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UtilityFunctions {
    public RequestRole cnvBeanToEntity(RequestRoleDto requestRoleDto) {
        RequestRole requestRole = new RequestRole();
        BeanUtils.copyProperties(requestRoleDto, requestRole);
        if(Objects.nonNull(requestRoleDto.getDoctorDto()))requestRole.setDoctor(cnvBeanToEntityDoctor(requestRoleDto.getDoctorDto()));
        return requestRole;
    }

    public RequestRoleDto cnvEntityToBean(RequestRole requestRole) {
        RequestRoleDto requestRoleDto = new RequestRoleDto();
        BeanUtils.copyProperties(requestRole, requestRoleDto);
        if(Objects.nonNull(requestRole.getDoctor()))requestRoleDto.setDoctorDto(cnvEntityToBeanDoctor(requestRole.getDoctor()));
        return requestRoleDto;
    }

    public DoctorDto cnvEntityToBeanDoctor(Doctor doctor) {
        DoctorDto doctorDto = new DoctorDto();
        BeanUtils.copyProperties(doctor, doctorDto);
        return doctorDto;
    }

    public Doctor cnvBeanToEntityDoctor(DoctorDto doctorDto) {
        Doctor doctor = new Doctor();
        BeanUtils.copyProperties(doctorDto, doctor);
        return doctor;
    }

    public Boolean validateRequestAdmin(String email, String role) {
        return Objects.equals(role, "ADMIN") && validateEmail(email);
    }

    private boolean validateEmail(String email) {
        return email != null && email.contains("@");
    }
}
