package com.project.appointmentService.Service;

import com.project.appointmentService.Entity.Appointment;
import com.project.appointmentService.Exceptions.AppointmentNotFoundException;
import com.project.appointmentService.Model.AppointmentDto;
import com.project.appointmentService.Model.Status;
import com.project.appointmentService.Model.VisitDetails;
import com.project.appointmentService.Repository.AppointmentRepository;
import com.project.appointmentService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@AllArgsConstructor
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private AppointmentRepository appointmentRepository;
    private UtilityFunctions utilityFunctions;
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public void markCancelled(String appointmentId, String cancelledBy) throws AppointmentNotFoundException {
        log.info("action=MARK_CANCELLED status=INITIATED identifier={} cancelledBy={}", appointmentId, cancelledBy);
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        if (appointmentOptional.isEmpty()) throw new AppointmentNotFoundException(appointmentId);
        Appointment appointment = appointmentOptional.get();
        appointment.setStatus(Status.CANCELLED);
        appointment.setDescription("Appointment cancelled by: " + cancelledBy);
        appointmentRepository.save(appointment);
        log.info("action=MARK_CANCELLED status=SUCCESS identifier={}", appointmentId);
    }

    public void restoreAppointment(String appointmentId) throws AppointmentNotFoundException {
        log.info("action=RESTORE_APPOINTMENT status=INITIATED identifier={}", appointmentId);
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        if (appointmentOptional.isEmpty()) throw new AppointmentNotFoundException(appointmentId);
        Appointment appointment = appointmentOptional.get();
        appointment.setStatus(Status.UPCOMING);
        appointment.setDescription(null);
        appointmentRepository.save(appointment);
        log.info("action=RESTORE_APPOINTMENT status=SUCCESS identifier={}", appointmentId);
    }

    public void deleteAppointment(String appointmentId) throws AppointmentNotFoundException {
        log.info("action=DELETE_APPOINTMENT status=INITIATED identifier={}", appointmentId);
        if (!appointmentRepository.existsById(appointmentId)) throw new AppointmentNotFoundException(appointmentId);
        appointmentRepository.deleteById(appointmentId);
        log.info("action=DELETE_APPOINTMENT status=SUCCESS identifier={}", appointmentId);
    }

    public AppointmentDto saveAppointment(AppointmentDto appointmentDto) {
        log.info("action=BOOK_APPOINTMENT status=INITIATED patientId={} doctorId={} date={}", appointmentDto.getPatientId(), appointmentDto.getDoctorId(), appointmentDto.getDate());
        Appointment saved = appointmentRepository.save(utilityFunctions.cnvDtoToEntity(appointmentDto));
        log.info("action=BOOK_APPOINTMENT status=SUCCESS id={} patientId={} doctorId={}", saved.getId(), saved.getPatientId(), saved.getDoctorId());
        return utilityFunctions.cnvEntityToDto(saved);
    }

    public AppointmentDto completeAppointment(VisitDetails visitDetails) throws AppointmentNotFoundException {
        log.info("action=COMPLETE_APPOINTMENT status=INITIATED identifier={}", visitDetails.getAppointmentId());
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(visitDetails.getAppointmentId());
        if (appointmentOptional.isEmpty()) throw new AppointmentNotFoundException(visitDetails.getAppointmentId());
        Appointment appointment = appointmentOptional.get();
        appointment.setStatus(Status.VISITED);
        appointment.setVisitDetails(visitDetails);
        Appointment saved = appointmentRepository.save(appointment);
        kafkaTemplate.send(MessageBuilder.withPayload(UtilityFunctions.cnvDtoToMap(saved))
                .setHeader(KafkaHeaders.TOPIC, "appointment-completed")
                .setHeader("X-Correlation-ID", MDC.get("correlationId"))
                .build());
        log.info("action=COMPLETE_APPOINTMENT status=SUCCESS identifier={} patientId={} doctorId={}", visitDetails.getAppointmentId(), saved.getPatientId(), saved.getDoctorId());
        return utilityFunctions.cnvEntityToDto(saved);
    }

    public AppointmentDto getAppointmentById(String appointmentId) throws AppointmentNotFoundException {
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        if (appointmentOptional.isEmpty()) throw new AppointmentNotFoundException(appointmentId);
        return utilityFunctions.cnvEntityToDto(appointmentOptional.get());
    }

    public List<AppointmentDto> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(a -> utilityFunctions.cnvEntityToDto(a))
                .toList();
    }

    public List<AppointmentDto> getAppointmentsByUserId(String userId) {
        return appointmentRepository.findByPatientId(userId).stream()
                .map(a -> utilityFunctions.cnvEntityToDto(a))
                .toList();
    }

    public List<AppointmentDto> getAppointmentsByDoctorId(String doctorId) {
        return appointmentRepository.findByDoctorId(doctorId).stream()
                .map(a -> utilityFunctions.cnvEntityToDto(a))
                .toList();
    }
}
