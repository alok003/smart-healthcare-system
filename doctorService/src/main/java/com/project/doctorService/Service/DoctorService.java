package com.project.doctorService.Service;

import com.project.doctorService.Entity.Doctor;
import com.project.doctorService.Exceptions.DateOutOfRangeException;
import com.project.doctorService.Exceptions.DoctorNotFoundException;
import com.project.doctorService.Model.*;
import com.project.doctorService.Repository.DoctorRepository;
import com.project.doctorService.Utility.UtilityFunctions;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);
    private static final int DAYS_TO_INITIALIZE = 31;

    private DoctorRepository doctorRepository;
    private UtilityFunctions utilityFunctions;
    private ExternalServiceClient externalServiceClient;
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public DoctorDto saveRequest(DoctorDto doctorDto, int maxCount, double rate) {
        log.info("action=SAVE_DOCTOR status=INITIATED identifier={}", doctorDto.getEmail());
        Optional<Doctor> doctor = doctorRepository.findByEmail(doctorDto.getEmail());
        if (doctor.isEmpty()) {
            Bookings bookings = new Bookings();
            bookings.setBookingListMap(setInitialBookingTemplate());
            bookings.setRate(rate);
            bookings.setMaxCount(maxCount);
            doctorDto.setBookings(bookings);
            Doctor saved = doctorRepository.save(utilityFunctions.cnvBeanToEntity(doctorDto));
            log.info("action=SAVE_DOCTOR status=SUCCESS identifier={} id={}", doctorDto.getEmail(), saved.getId());
            return utilityFunctions.cnvEntityToBean(saved);
        } else {
            log.debug("action=SAVE_DOCTOR status=SKIPPED identifier={} reason=ALREADY_EXISTS", doctorDto.getEmail());
            return utilityFunctions.cnvEntityToBean(doctor.get());
        }
    }

    private Map<LocalDate, BookingList> setInitialBookingTemplate() {
        Map<LocalDate, BookingList> bookingListMap = new HashMap<>();
        LocalDate todayDate = LocalDate.now();
        for (int i = 0; i < DAYS_TO_INITIALIZE; i++) {
            bookingListMap.put(todayDate, BookingList.builder().availibility(Availibility.AVAILABLE).build());
            todayDate = todayDate.plusDays(1);
        }
        return bookingListMap;
    }

    public DoctorDto addLeave(String email, List<LocalDate> leave) throws DoctorNotFoundException {
        log.info("action=ADD_LEAVE status=INITIATED identifier={} days={}", email, leave.size());
        Optional<Doctor> doctor = doctorRepository.findByEmail(email);
        if (doctor.isEmpty()) throw new DoctorNotFoundException();
        Map<LocalDate, BookingList> bookingListMap = doctor.get().getBookings().getBookingListMap();
        for (LocalDate day : leave) {
            if (!bookingListMap.containsKey(day)) continue;
            BookingList slot = bookingListMap.get(day);
            if (slot.getAvailibility() == Availibility.UNAVAILABLE) {
                log.debug("action=ADD_LEAVE detail=SKIPPED_ALREADY_UNAVAILABLE identifier={} date={}", email, day);
                continue;
            }
            List<String> appointmentIds = new ArrayList<>(slot.getBookingId());
            List<String> successfullyCleaned = new ArrayList<>();
            boolean allCleared = true;
            for (String appointmentId : appointmentIds) {
                try {
                    externalServiceClient.markCancelled(appointmentId, email, email, UserRole.ADMIN.name());
                    log.debug("action=ADD_LEAVE detail=APPOINTMENT_CANCELLED identifier={} date={} appointmentId={}", email, day, appointmentId);
                } catch (Exception e) {
                    log.warn("action=ADD_LEAVE status=PARTIAL_FAIL identifier={} date={} appointmentId={} reason=CANCEL_FAILED error={}", email, day, appointmentId, e.getMessage());
                    allCleared = false;
                    break;
                }
                try {
                    externalServiceClient.removeAppointmentFromPatient(appointmentId, email, UserRole.ADMIN.name());
                    log.debug("action=ADD_LEAVE detail=PATIENT_UPDATED identifier={} date={} appointmentId={}", email, day, appointmentId);
                    successfullyCleaned.add(appointmentId);
                    kafkaTemplate.send(MessageBuilder
                            .withPayload(Map.of("appointmentId", appointmentId, "cancelledBy", email, "date", day.toString()))
                            .setHeader(KafkaHeaders.TOPIC, "appointment-cancelled-notification")
                            .setHeader("X-Correlation-ID", MDC.get("correlationId"))
                            .build());
                    log.debug("action=KAFKA_PUBLISH status=SUCCESS topic=appointment-cancelled-notification appointmentId={}", appointmentId);
                } catch (Exception e) {
                    log.warn("action=ADD_LEAVE status=PARTIAL_FAIL identifier={} date={} appointmentId={} reason=PATIENT_REMOVE_FAILED error={}", email, day, appointmentId, e.getMessage());
                    try {
                        externalServiceClient.restoreAppointment(appointmentId, email, UserRole.ADMIN.name());
                        log.debug("action=ADD_LEAVE detail=APPOINTMENT_RESTORED identifier={} date={} appointmentId={}", email, day, appointmentId);
                    } catch (Exception restoreEx) {
                        log.error("action=ADD_LEAVE status=ROLLBACK_FAILED identifier={} date={} appointmentId={} reason=RESTORE_FAILED error={}", email, day, appointmentId, restoreEx.getMessage());
                    }
                    allCleared = false;
                    break;
                }
            }
            slot.getBookingId().removeAll(successfullyCleaned);
            if (allCleared) {
                slot.setAvailibility(Availibility.UNAVAILABLE);
                log.debug("action=ADD_LEAVE detail=DATE_MARKED_UNAVAILABLE identifier={} date={}", email, day);
            } else {
                log.warn("action=ADD_LEAVE detail=DATE_NOT_FULLY_CLEARED identifier={} date={} clearedCount={} totalCount={}", email, day, successfullyCleaned.size(), appointmentIds.size());
            }
        }
        Doctor doctorSave = doctor.get();
        doctorSave.getBookings().setBookingListMap(bookingListMap);
        DoctorDto result = utilityFunctions.cnvEntityToBean(doctorRepository.save(doctorSave));
        log.info("action=ADD_LEAVE status=SUCCESS identifier={}", email);
        return result;
    }

    public List<DoctorDto> getAllDoctors() {
        return doctorRepository.findAll().stream().map(utilityFunctions::cnvEntityToBean).toList();
    }

    public Boolean addDocAppointment(AppointmentDto appointmentDto, String email) throws DoctorNotFoundException, DateOutOfRangeException {
        log.info("action=ADD_DOC_APPOINTMENT status=INITIATED doctorId={} appointmentId={}", appointmentDto.getDoctorId(), appointmentDto.getId());
        Optional<Doctor> doctor = doctorRepository.findByEmail(appointmentDto.getDoctorId());
        if (doctor.isEmpty()) throw new DoctorNotFoundException();
        Map<LocalDate, BookingList> newList = doctor.get().getBookings().getBookingListMap();
        LocalDate appointmentDate = appointmentDto.getDate();
        if (!newList.containsKey(appointmentDate)) throw new DateOutOfRangeException();
        BookingList slot = newList.get(appointmentDate);
        if (slot.getBookingId() == null) slot.setBookingId(new ArrayList<>());
        slot.getBookingId().add(appointmentDto.getId());
        if (slot.getBookingId().size() >= doctor.get().getBookings().getMaxCount()) {
            slot.setAvailibility(Availibility.BOOKED);
            log.debug("action=ADD_DOC_APPOINTMENT detail=SLOT_FULLY_BOOKED doctorId={} date={}", appointmentDto.getDoctorId(), appointmentDate);
        }
        Doctor doctorSave = doctor.get();
        doctorSave.getBookings().setBookingListMap(newList);
        doctorRepository.save(doctorSave);
        log.info("action=ADD_DOC_APPOINTMENT status=SUCCESS doctorId={} appointmentId={} date={}", appointmentDto.getDoctorId(), appointmentDto.getId(), appointmentDate);
        return true;
    }

    public AppointmentDto completeAppointment(VisitDetails visitDetails, String email) throws DoctorNotFoundException {
        log.info("action=COMPLETE_APPOINTMENT status=INITIATED identifier={} requestedBy={}", visitDetails.getAppointmentId(), email);
        AppointmentDto appointment = externalServiceClient.getAppointmentById(visitDetails.getAppointmentId(), email, UserRole.ADMIN.name());
        if (!appointment.getDoctorId().equals(email)) {
            log.warn("action=COMPLETE_APPOINTMENT status=REJECTED identifier={} reason=DOCTOR_MISMATCH requestedBy={}", visitDetails.getAppointmentId(), email);
            throw new DoctorNotFoundException();
        }
        AppointmentDto result = externalServiceClient.completeAppointment(visitDetails, email, UserRole.ADMIN.name());
        log.info("action=COMPLETE_APPOINTMENT status=SUCCESS identifier={} requestedBy={}", visitDetails.getAppointmentId(), email);
        return result;
    }

    public List<AppointmentDto> getMyAppointments(String email) {
        return externalServiceClient.getAppointmentsByDoctor(email, email, UserRole.ADMIN.name());
    }

    public DoctorDto getMyDetails(String email) throws DoctorNotFoundException {
        Optional<Doctor> doctor = doctorRepository.findByEmail(email);
        if (doctor.isEmpty()) throw new DoctorNotFoundException();
        return utilityFunctions.cnvEntityToBean(doctor.get());
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void refreshBookingSchedules() {
        log.info("action=REFRESH_BOOKING_SCHEDULES status=INITIATED");
        List<Doctor> doctors = doctorRepository.findAll();
        LocalDate today = LocalDate.now();
        for (Doctor doctor : doctors) {
            Map<LocalDate, BookingList> bookingListMap = doctor.getBookings().getBookingListMap();
            bookingListMap.entrySet().removeIf(entry -> entry.getKey().isBefore(today));
            int added = 0;
            for (int i = 0; i < DAYS_TO_INITIALIZE; i++) {
                LocalDate day = today.plusDays(i);
                if (!bookingListMap.containsKey(day)) {
                    bookingListMap.put(day, BookingList.builder().availibility(Availibility.AVAILABLE).build());
                    added++;
                    log.debug("action=REFRESH_BOOKING_SCHEDULES detail=SLOT_ADDED identifier={} date={}", doctor.getEmail(), day);
                }
            }
            doctor.getBookings().setBookingListMap(bookingListMap);
            doctorRepository.save(doctor);
            log.debug("action=REFRESH_BOOKING_SCHEDULES detail=UPDATED identifier={} slotsAdded={}", doctor.getEmail(), added);
        }
        log.info("action=REFRESH_BOOKING_SCHEDULES status=SUCCESS doctorCount={}", doctors.size());
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void sendDailyScheduleNotification() {
        log.info("action=DAILY_SCHEDULE_NOTIFICATION status=INITIATED");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Doctor> doctors = doctorRepository.findAll();
        for (Doctor doctor : doctors) {
            BookingList slot = doctor.getBookings().getBookingListMap().get(tomorrow);
            if (slot == null || slot.getBookingId().isEmpty()) continue;
            kafkaTemplate.send(MessageBuilder
                    .withPayload(Map.of(
                            "doctorEmail", doctor.getEmail(),
                            "date", tomorrow.toString(),
                            "appointmentIds", slot.getBookingId()
                    ))
                    .setHeader(KafkaHeaders.TOPIC, "doctor-daily-schedule")
                    .setHeader("X-Correlation-ID", UUID.randomUUID().toString())
                    .build());
            log.debug("action=DAILY_SCHEDULE_NOTIFICATION detail=PUBLISHED identifier={} date={} count={}", doctor.getEmail(), tomorrow, slot.getBookingId().size());
        }
        log.info("action=DAILY_SCHEDULE_NOTIFICATION status=SUCCESS doctorCount={}", doctors.size());
    }

    public void removeAppointmentFromSchedule(String appointmentId, String doctorId, String dateStr) {
        log.info("action=REMOVE_APPOINTMENT_FROM_SCHEDULE status=INITIATED identifier={} doctorId={}", appointmentId, doctorId);
        Optional<Doctor> doctorOpt = doctorRepository.findByEmail(doctorId);
        if (doctorOpt.isEmpty()) {
            log.warn("action=REMOVE_APPOINTMENT_FROM_SCHEDULE status=SKIPPED identifier={} reason=DOCTOR_NOT_FOUND", doctorId);
            return;
        }
        LocalDate date = LocalDate.parse(dateStr);
        Doctor doctor = doctorOpt.get();
        Map<LocalDate, BookingList> bookingListMap = doctor.getBookings().getBookingListMap();
        if (!bookingListMap.containsKey(date)) {
            log.warn("action=REMOVE_APPOINTMENT_FROM_SCHEDULE status=SKIPPED identifier={} date={} reason=DATE_NOT_FOUND", appointmentId, date);
            return;
        }
        BookingList slot = bookingListMap.get(date);
        slot.getBookingId().remove(appointmentId);
        if (slot.getAvailibility() == Availibility.BOOKED) {
            slot.setAvailibility(Availibility.AVAILABLE);
            log.debug("action=REMOVE_APPOINTMENT_FROM_SCHEDULE detail=SLOT_REOPENED doctorId={} date={}", doctorId, date);
        }
        doctorRepository.save(doctor);
        log.info("action=REMOVE_APPOINTMENT_FROM_SCHEDULE status=SUCCESS identifier={} doctorId={} date={}", appointmentId, doctorId, date);
    }
}
