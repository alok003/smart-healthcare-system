package com.project.patientService.Service;

import com.project.patientService.Entity.Patient;
import com.project.patientService.Exceptions.PatientNotFoundException;
import com.project.patientService.Model.*;
import com.project.patientService.Repository.PatientRepository;
import com.project.patientService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private PatientRepository patientRepository;
    private UtilityFunctions utilityFunctions;
    private ExternalServiceClient externalServiceClient;
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public PatientDto savePatient(PatientDto patientDto) {
        log.info("action=SAVE_PATIENT status=INITIATED identifier={}", patientDto.getEmail());
        Optional<Patient> patient = patientRepository.findByEmail(patientDto.getEmail());
        if (patient.isEmpty()) {
            Patient newPatient = utilityFunctions.convertToPatient(patientDto);
            patientRepository.save(newPatient);
            log.info("action=SAVE_PATIENT status=SUCCESS identifier={}", patientDto.getEmail());
            return utilityFunctions.convertToPatientDto(newPatient);
        } else {
            log.debug("action=SAVE_PATIENT status=SKIPPED identifier={} reason=ALREADY_EXISTS", patientDto.getEmail());
            return utilityFunctions.convertToPatientDto(patient.get());
        }
    }

    public PatientDto getMyProfile(String email) throws PatientNotFoundException {
        Optional<Patient> patient = patientRepository.findByEmail(email);
        if (patient.isEmpty()) throw new PatientNotFoundException(email);
        return utilityFunctions.convertToPatientDto(patient.get());
    }

    public AppointmentDto bookAppointment(AppointmentDto appointmentDto, String email) throws PatientNotFoundException {
        log.info("action=BOOK_APPOINTMENT status=INITIATED identifier={} doctorId={} date={}", email, appointmentDto.getDoctorId(), appointmentDto.getDate());
        Optional<Patient> patientOpt = patientRepository.findByEmail(email);
        if (patientOpt.isEmpty()) throw new PatientNotFoundException(email);
        Patient patient = patientOpt.get();
        DoctorDto doctor = externalServiceClient.getDoctorByEmail(appointmentDto.getDoctorId(), email, UserRole.ADMIN.name());
        log.debug("action=BOOK_APPOINTMENT detail=DOCTOR_FETCHED identifier={} doctorId={}", email, appointmentDto.getDoctorId());
        BookingList slot = doctor.getBookings().getBookingListMap().get(appointmentDto.getDate());
        if (slot == null || slot.getAvailibility() != Availibility.AVAILABLE) {
            log.warn("action=BOOK_APPOINTMENT status=REJECTED identifier={} doctorId={} date={} reason=SLOT_UNAVAILABLE", email, appointmentDto.getDoctorId(), appointmentDto.getDate());
            throw new RuntimeException("Selected slot is not available for date: " + appointmentDto.getDate());
        }
        appointmentDto.setPatientId(email);
        appointmentDto.setStatus(Status.UPCOMING);
        AppointmentDto saved = externalServiceClient.bookAppointment(appointmentDto, email, UserRole.ADMIN.name());
        log.debug("action=BOOK_APPOINTMENT detail=APPOINTMENT_SAVED identifier={} appointmentId={}", email, saved.getId());
        try {
            patient.getAppointmentList().add(saved.getId());
            patientRepository.save(patient);
            log.debug("action=BOOK_APPOINTMENT detail=PATIENT_UPDATED identifier={} appointmentId={}", email, saved.getId());
            externalServiceClient.addDocAppointment(saved, email, UserRole.ADMIN.name());
            log.debug("action=BOOK_APPOINTMENT detail=DOCTOR_UPDATED identifier={} appointmentId={}", email, saved.getId());
        } catch (Exception e) {
            log.error("action=BOOK_APPOINTMENT status=ROLLBACK identifier={} appointmentId={} reason={}", email, saved.getId(), e.getMessage());
            try {
                patient.getAppointmentList().remove(saved.getId());
                patientRepository.save(patient);
            } catch (Exception rollbackEx) {
                log.error("action=BOOK_APPOINTMENT status=ROLLBACK_PARTIAL identifier={} appointmentId={} reason=PATIENT_ROLLBACK_FAILED error={}", email, saved.getId(), rollbackEx.getMessage());
            }
            try {
                externalServiceClient.deleteAppointment(saved.getId(), email, UserRole.ADMIN.name());
            } catch (Exception rollbackEx) {
                log.error("action=BOOK_APPOINTMENT status=ROLLBACK_PARTIAL identifier={} appointmentId={} reason=DELETE_FAILED error={}", email, saved.getId(), rollbackEx.getMessage());
            }
            throw new RuntimeException("Booking failed due to an internal error. Please try again.");
        }
        kafkaTemplate.send(MessageBuilder.withPayload(UtilityFunctions.cnvDtoToMap(saved))
                .setHeader(KafkaHeaders.TOPIC, "appointment-booked")
                .setHeader("X-Correlation-ID", MDC.get("correlationId"))
                .build());
        log.info("action=KAFKA_PUBLISH status=SUCCESS topic=appointment-booked identifier={} appointmentId={}", email, saved.getId());
        log.info("action=BOOK_APPOINTMENT status=SUCCESS identifier={} appointmentId={} doctorId={}", email, saved.getId(), saved.getDoctorId());
        return saved;
    }

    public AppointmentDto cancelAppointment(String id, String email) throws PatientNotFoundException {
        log.info("action=CANCEL_APPOINTMENT status=INITIATED identifier={} requestedBy={}", id, email);
        Optional<Patient> patientOpt = patientRepository.findByEmail(email);
        if (patientOpt.isEmpty()) throw new PatientNotFoundException(email);
        Patient patient = patientOpt.get();
        if (!patient.getAppointmentList().contains(id)) {
            log.warn("action=CANCEL_APPOINTMENT status=REJECTED identifier={} requestedBy={} reason=NOT_OWNED", id, email);
            throw new PatientNotFoundException("Appointment does not belong to patient: " + email);
        }
        AppointmentDto appointment = externalServiceClient.getAppointmentById(id, email, UserRole.ADMIN.name());
        externalServiceClient.markCancelled(id, email, email, UserRole.ADMIN.name());
        log.debug("action=CANCEL_APPOINTMENT detail=APPOINTMENT_MARKED_CANCELLED identifier={}", id);
        try {
            externalServiceClient.removeAppointmentFromSchedule(id, appointment.getDate().toString(), email, UserRole.ADMIN.name());
            log.debug("action=CANCEL_APPOINTMENT detail=DOCTOR_SCHEDULE_UPDATED identifier={}", id);
        } catch (Exception e) {
            log.error("action=CANCEL_APPOINTMENT status=ROLLBACK identifier={} reason=DOCTOR_UPDATE_FAILED error={}", id, e.getMessage());
            try {
                externalServiceClient.restoreAppointment(id, email, UserRole.ADMIN.name());
            } catch (Exception restoreEx) {
                log.error("action=CANCEL_APPOINTMENT status=ROLLBACK_FAILED identifier={} reason=RESTORE_FAILED error={}", id, restoreEx.getMessage());
            }
            throw new RuntimeException("Cancellation failed: unable to update doctor schedule.");
        }
        patient.getAppointmentList().remove(id);
        patientRepository.save(patient);
        kafkaTemplate.send(MessageBuilder.withPayload(UtilityFunctions.cnvDtoToMap(appointment))
                .setHeader(KafkaHeaders.TOPIC, "appointment-cancelled-notification")
                .setHeader("X-Correlation-ID", MDC.get("correlationId"))
                .build());
        log.info("action=KAFKA_PUBLISH status=SUCCESS topic=appointment-cancelled-notification identifier={}", id);
        log.info("action=CANCEL_APPOINTMENT status=SUCCESS identifier={} requestedBy={}", id, email);
        return appointment;
    }

    public List<AppointmentDto> getAllAppointmentsbyPatient(String email) {
        return externalServiceClient.getAppointmentsByPatient(email, email, UserRole.ADMIN.name());
    }

    public String getPrescription(String id, String email) throws PatientNotFoundException {
        log.info("action=GET_PRESCRIPTION status=INITIATED identifier={} requestedBy={}", id, email);
        AppointmentDto appointmentDto = externalServiceClient.getAppointmentById(id, email, UserRole.ADMIN.name());
        if (appointmentDto.getVisitDetails() != null && appointmentDto.getPatientId().equals(email)) {
            kafkaTemplate.send(MessageBuilder.withPayload(UtilityFunctions.cnvDtoToMap(appointmentDto))
                    .setHeader(KafkaHeaders.TOPIC, "send-email-appointment")
                    .setHeader("X-Correlation-ID", MDC.get("correlationId"))
                    .build());
            log.info("action=GET_PRESCRIPTION status=SUCCESS identifier={} requestedBy={}", id, email);
            return "Prescription for Appointment has been sent out to Email";
        } else {
            log.warn("action=GET_PRESCRIPTION status=NOT_AVAILABLE identifier={} requestedBy={}", id, email);
            return "No prescription available for this appointment";
        }
    }

    public List<DoctorDto> getDoctorAvailibility(String email) {
        List<DoctorDto> doctors = externalServiceClient.getAllDoctors(email, UserRole.ADMIN.name());
        doctors.forEach(doctor -> doctor.getBookings().getBookingListMap()
                .values().forEach(slot -> slot.getBookingId().clear()));
        return doctors;
    }

    public void removeAppointmentFromPatientByAppointmentId(String appointmentId) {
        log.info("action=REMOVE_APPOINTMENT_FROM_PATIENT status=INITIATED identifier={}", appointmentId);
        Optional<Patient> patientOpt = patientRepository.findByAppointmentListContaining(appointmentId);
        if (patientOpt.isEmpty()) {
            log.warn("action=REMOVE_APPOINTMENT_FROM_PATIENT status=SKIPPED identifier={} reason=PATIENT_NOT_FOUND", appointmentId);
            return;
        }
        Patient patient = patientOpt.get();
        patient.getAppointmentList().remove(appointmentId);
        patientRepository.save(patient);
        log.info("action=REMOVE_APPOINTMENT_FROM_PATIENT status=SUCCESS identifier={} patientId={}", appointmentId, patient.getEmail());
    }

    public void updatePatientVitals(String appointmentId, String patientId, String dateStr, Map<String, Object> healthCheckMap) {
        log.info("action=UPDATE_PATIENT_VITALS status=INITIATED identifier={} patientId={}", appointmentId, patientId);
        Optional<Patient> patientOpt = patientRepository.findByEmail(patientId);
        if (patientOpt.isEmpty()) {
            log.warn("action=UPDATE_PATIENT_VITALS status=SKIPPED identifier={} reason=PATIENT_NOT_FOUND", patientId);
            return;
        }
        HealthCheck healthCheck = UtilityFunctions.cnvMapToDto(healthCheckMap, HealthCheck.class);
        if (healthCheck == null) {
            log.warn("action=UPDATE_PATIENT_VITALS status=SKIPPED identifier={} reason=HEALTH_CHECK_NULL", appointmentId);
            return;
        }
        Patient patient = patientOpt.get();
        patient.getVitalsFlow().put(LocalDate.parse(dateStr), healthCheck);
        patientRepository.save(patient);
        log.info("action=UPDATE_PATIENT_VITALS status=SUCCESS identifier={} patientId={} date={}", appointmentId, patientId, dateStr);
    }
}
