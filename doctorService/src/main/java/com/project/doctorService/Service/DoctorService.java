package com.project.doctorService.Service;

import com.project.doctorService.Entity.Doctor;
import com.project.doctorService.Exceptions.DoctorNotFoundException;
import com.project.doctorService.Model.Availibility;
import com.project.doctorService.Model.BookingList;
import com.project.doctorService.Model.DoctorDto;
import com.project.doctorService.Model.UserRole;
import com.project.doctorService.RESTCalls.AppointmentClient;
import com.project.doctorService.Repository.DoctorRepository;
import com.project.doctorService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;


@Service
@AllArgsConstructor
public class DoctorService {
    private static final int DAYS_TO_INITIALIZE = 31;
    private DoctorRepository doctorRepository;
    private UtilityFunctions utilityFunctions;
    private AppointmentClient appointmentClient;

    public DoctorDto saveRequest(DoctorDto doctorDto) {
        Optional<Doctor> doctor=doctorRepository.findByEmail(doctorDto.getEmail());
        if(doctor.isEmpty()){
            doctorDto.getBookings().setBookingListMap(setInitialBookingTemplate());
            Doctor saved=doctorRepository.save(utilityFunctions.cnvBeanToEntity(doctorDto));
            return utilityFunctions.cnvEntityToBean(saved);
        }else return utilityFunctions.cnvEntityToBean(doctor.get());
    }
    private Map<LocalDate, BookingList> setInitialBookingTemplate(){
        Map<LocalDate,BookingList> bookingListMap=new HashMap<>();
        LocalDate todayDate=LocalDate.now();
        for(int i=0;i<DAYS_TO_INITIALIZE;i++){
            BookingList bookingList=BookingList.builder().availibility(Availibility.AVAILABLE).build();
            bookingListMap.put(todayDate,bookingList);
            todayDate=todayDate.plusDays(1);
        }
        return bookingListMap;
    }

    public DoctorDto addLeave(String email, List<LocalDate> leave) throws DoctorNotFoundException {
        Optional<Doctor> doctor=doctorRepository.findByEmail(email);
        if(doctor.isPresent()){
            Map<LocalDate,BookingList> newList=doctor.get().getBookings().getBookingListMap();
            for(LocalDate days:leave){
                if(newList.containsKey(days)){
                    cancelAppointments(newList.get(days).getBookingId(),email);
                    newList.get(days).setAvailibility(Availibility.UNAVAILABLE);
                    newList.get(days).setBookingId(new ArrayList<>());
                }
            }
            Doctor doctorsave=doctor.get();
            doctorsave.getBookings().setBookingListMap(newList);
            return utilityFunctions.cnvEntityToBean(doctorRepository.save(doctorsave));
        }else throw new DoctorNotFoundException();
    }

    public void cancelAppointments(List<String> bookingIds,String email){
        for(String bId:bookingIds){
            appointmentClient.cancelAppointmentAppointmentClient(email, UserRole.ADMIN.name(),bId);
        }
    }

    public List<DoctorDto> getAllDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors
                .stream()
                .map(doc -> utilityFunctions.cnvEntityToBean(doc))
                .toList();
    }
}
