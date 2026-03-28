package com.project.appointmentService.Service;

import com.project.appointmentService.Entity.Appointment;
import com.project.appointmentService.Exception.AppointmentNotFoundException;
import com.project.appointmentService.Model.AppointmentDto;
import com.project.appointmentService.Model.Status;
import com.project.appointmentService.Model.VisitDetails;
import com.project.appointmentService.Repository.AppointmentRepository;
import com.project.appointmentService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AppointmentService {

    private AppointmentRepository appointmentRepository;
    private UtilityFunctions utilityFunctions;

    public AppointmentDto saveAppointment(AppointmentDto appointmentDto) {
        return utilityFunctions.cnvEntityToDto(appointmentRepository.save(utilityFunctions.cnvDtoToEntity(appointmentDto, Appointment.class)), AppointmentDto.class);
    }

    public AppointmentDto cancelAppointment(String appointmentId,String email) throws AppointmentNotFoundException {
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        if (appointmentOptional.isPresent()) {
            Appointment appointment = appointmentOptional.get();
            appointment.setStatus(Status.CANCELLED);
            appointment.setDescription("Appointment cancelled by User: "+email);
            return utilityFunctions.cnvEntityToDto(appointmentRepository.save(appointment), AppointmentDto.class);
        } else {
            throw new AppointmentNotFoundException(appointmentId);
        }
    }

    public AppointmentDto completeAppointment(VisitDetails visitDetails) throws AppointmentNotFoundException{
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(visitDetails.getAppointmentId());
        if (appointmentOptional.isPresent()) {
            Appointment appointment = appointmentOptional.get();
            appointment.setStatus(Status.VISITED);
            appointment.setVisitDetails(visitDetails);
            return utilityFunctions.cnvEntityToDto(appointmentRepository.save(appointment), AppointmentDto.class);
        } else {
            throw new AppointmentNotFoundException(visitDetails.getAppointmentId());
        }
    }

    public AppointmentDto getAppointmentById(String appointmentId) throws AppointmentNotFoundException {
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        if (appointmentOptional.isPresent()) {
            return utilityFunctions.cnvEntityToDto(appointmentOptional.get(), AppointmentDto.class);
        } else {
            throw new AppointmentNotFoundException(appointmentId);
        }
    }

    public List<AppointmentDto> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(appointment -> utilityFunctions.cnvEntityToDto(appointment, AppointmentDto.class))
                .toList();
    }

    public List<AppointmentDto> getAppointmentsByUserId(String userId) {
        return appointmentRepository.findByPatientId(userId)
                .stream()
                .map(appointment -> utilityFunctions.cnvEntityToDto(appointment, AppointmentDto.class))
                .toList();
    }

    public List<AppointmentDto> getAppointmentsByDoctorId(String doctorId) {
        return appointmentRepository.findByDoctorId(doctorId)
                .stream()
                .map(appointment -> utilityFunctions.cnvEntityToDto(appointment, AppointmentDto.class))
                .toList();
    }



}
