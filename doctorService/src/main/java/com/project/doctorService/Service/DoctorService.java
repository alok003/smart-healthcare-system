package com.project.doctorService.Service;

import com.project.doctorService.Entity.Doctor;
import com.project.doctorService.Model.DoctorDto;
import com.project.doctorService.Repository.DoctorRepository;
import com.project.doctorService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@AllArgsConstructor
public class DoctorService {
    private DoctorRepository doctorRepository;
    private UtilityFunctions utilityFunctions;

    public DoctorDto saveRequest(DoctorDto doctorDto) {
        Optional<Doctor> doctor=doctorRepository.findByEmail(doctorDto.getEmail());
        if(doctor.isEmpty()){
            Doctor saved=doctorRepository.save(utilityFunctions.cnvBeanToEntity(doctorDto));
            return utilityFunctions.cnvEntityToBean(saved);
        }else return utilityFunctions.cnvEntityToBean(doctor.get());
    }
}
