package com.project.doctorService.Utility;

import com.project.doctorService.Entity.Doctor;
import com.project.doctorService.Model.DoctorDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UtilityFunctions {
    public Doctor cnvBeanToEntity(DoctorDto doctorDto) {
        Doctor doctor = new Doctor();
        BeanUtils.copyProperties(doctorDto, doctor);
        return doctor;
    }

    public DoctorDto cnvEntityToBean(Doctor doctor) {
        DoctorDto doctorDto = new DoctorDto();
        BeanUtils.copyProperties(doctor, doctorDto);
        return doctorDto;
    }

    public Boolean validateRequestAdmin(String email,String role){
        return Objects.equals(role, "ADMIN")&&validateEmail(email);
    }

    private boolean validateEmail(String email) {
        return email != null && email.contains("@");
    }
}
