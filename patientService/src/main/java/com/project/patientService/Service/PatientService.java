package com.project.patientService.Service;

import com.project.patientService.Entity.Patient;
import com.project.patientService.Exception.PatientAlreadyExistsException;
import com.project.patientService.Exception.PatientNotFoundException;
import com.project.patientService.Model.*;
import com.project.patientService.RESTCalls.AppointmentClient;
import com.project.patientService.RESTCalls.DoctorClient;
import com.project.patientService.Repository.PatientRepository;
import com.project.patientService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PatientService {
    private PatientRepository patientRepository;
    private UtilityFunctions utilityFunctions;
    private DoctorClient doctorClient;
    private AppointmentClient appointmentClient;
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public PatientDto savePatient(PatientDto patientDto) {
        System.out.println("savePatient called for email: " + patientDto.getEmail());
        Optional<Patient> patient=patientRepository.findByEmail(patientDto.getEmail());
        if(patient.isEmpty()){
            Patient newPatient=utilityFunctions.convertToPatient(patientDto);
            patientRepository.save(newPatient);
            System.out.println("Patient saved: " + patientDto.getEmail());
            return utilityFunctions.convertToPatientDto(newPatient);
        } else {
            System.out.println("Patient already exists, returning existing: " + patientDto.getEmail());
            return utilityFunctions.convertToPatientDto(patient.get());
        }
    }

    public AppointmentDto bookAppointment(AppointmentDto appointmentDto, String email) throws PatientNotFoundException{
        System.out.println("bookAppointment called for patient: " + email + ", doctor: " + appointmentDto.getDoctorId());
        Optional<Patient> patient=patientRepository.findByEmail(email);
        if(patient.isEmpty()){throw new PatientNotFoundException(email);}
        appointmentDto.setPatientId(email);
        appointmentDto.setStatus(Status.UPCOMING);
        appointmentDto=appointmentClient.bookAppointment(appointmentDto,email,UserRole.ADMIN.name());
        System.out.println("Appointment booked with id: " + appointmentDto.getId());
        Patient patientEntity=patient.get();
        patientEntity.getAppointmentList().add(appointmentDto.getId());
        patientRepository.save(patientEntity);
        boolean docUpdated=doctorClient.addDocAppointment(appointmentDto,email,UserRole.ADMIN.name());
        System.out.println("Doctor schedule updated: " + docUpdated);
        return appointmentDto;
    }

     public AppointmentDto cancelAppointment(String id,String email) {
        return appointmentClient.cancelAppointmentAppointmentClient(id,email,UserRole.ADMIN.name());
     }

     public List<AppointmentDto> getAllAppointmentsbyPatient(String email) {
        return appointmentClient.getAllAppointmentsbyPatient(email,email,UserRole.ADMIN.name());
     }

    public String getPrescription(String id, String email) {
        System.out.println("getPrescription called for appointment: " + id + ", patient: " + email);
        AppointmentDto appointmentDto = appointmentClient.getAppointmentById(id, email, UserRole.ADMIN.name());
        if (appointmentDto.getVisitDetails() != null && appointmentDto.getPatientId().equals(email)) {
            Map<String, Object> message = utilityFunctions.cnvDtoToMap(appointmentDto);
            kafkaTemplate.send("send-email-appointment", message);
            System.out.println("Prescription email sent for appointment: " + id);
            return "Prescription for Appointment has been sent out to Email";
        } else {
            System.out.println("No prescription available for appointment: " + id);
            return "No prescription available for this appointment";
        }
    }

    public List<DoctorDto> getDoctorAvailibility(String email) {
        return doctorClient.getAllDoctorList(email, UserRole.ADMIN.name());
    }
}
