package com.project.doctorService.Service;

import com.project.doctorService.Entity.Doctor;
import com.project.doctorService.Exceptions.DateOutOfRangeException;
import com.project.doctorService.Exceptions.DoctorNotFoundException;
import com.project.doctorService.Model.*;
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

    public DoctorDto saveRequest(DoctorDto doctorDto, int maxCount, double rate) {
        System.out.println("saveDoctor called for email: " + doctorDto.getEmail());
        Optional<Doctor> doctor=doctorRepository.findByEmail(doctorDto.getEmail());
        if(doctor.isEmpty()){
            System.out.println("New doctor, saving to DB: " + doctorDto.getEmail());
            Bookings bookings = new Bookings();
            bookings.setBookingListMap(setInitialBookingTemplate());
            bookings.setRate(rate);
            bookings.setMaxCount(maxCount);
            doctorDto.setBookings(bookings);
            Doctor saved=doctorRepository.save(utilityFunctions.cnvBeanToEntity(doctorDto));
            System.out.println("Doctor saved with id: " + saved.getId());
            return utilityFunctions.cnvEntityToBean(saved);
        }else {
            System.out.println("Doctor already exists for email: " + doctorDto.getEmail() + ", skipping save");
            return utilityFunctions.cnvEntityToBean(doctor.get());
        }
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
        System.out.println("addLeave called for: " + email + ", days: " + leave);
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
            AppointmentDto appointmentDto=appointmentClient.cancelAppointmentAppointmentClient(bId,email, UserRole.ADMIN.name());
        }
    }

    public List<DoctorDto> getAllDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors
                .stream()
                .map(doc -> utilityFunctions.cnvEntityToBean(doc))
                .toList();
    }

    public Boolean addDocAppointment(AppointmentDto appointmentDto, String email) throws DoctorNotFoundException{
        System.out.println("addDocAppointment called for doctor: " + appointmentDto.getDoctorId() + ", appointment: " + appointmentDto.getId());
        Optional<Doctor> doctor=doctorRepository.findByEmail(appointmentDto.getDoctorId());
        if(doctor.isPresent()){
            Map<LocalDate,BookingList> newList=doctor.get().getBookings().getBookingListMap();
            LocalDate appointmentDate=appointmentDto.getDate();
            if(newList.containsKey(appointmentDate)){
                newList.get(appointmentDate).getBookingId().add(appointmentDto.getId());
                Doctor doctorsave=doctor.get();
                doctorsave.getBookings().setBookingListMap(newList);
                doctorRepository.save(doctorsave);
                return true;
            }else throw new DateOutOfRangeException();
        }else throw new DoctorNotFoundException();
    }

    public DoctorDto getMyDetails(String email) {
        Optional<Doctor> doctor=doctorRepository.findByEmail(email);
        if(doctor.isPresent()){
            return utilityFunctions.cnvEntityToBean(doctor.get());
        }else throw new DoctorNotFoundException();
    }
}
