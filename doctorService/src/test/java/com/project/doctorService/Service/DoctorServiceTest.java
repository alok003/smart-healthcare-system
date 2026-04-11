package com.project.doctorService.Service;

import com.project.doctorService.Entity.Doctor;
import com.project.doctorService.Model.Availibility;
import com.project.doctorService.Model.BookingList;
import com.project.doctorService.Model.Bookings;
import com.project.doctorService.Repository.DoctorRepository;
import com.project.doctorService.Utility.UtilityFunctions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UtilityFunctions utilityFunctions;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @InjectMocks
    private DoctorService doctorService;

    private Doctor buildDoctorWithSlots(String email, Map<LocalDate, BookingList> slots) {
        Doctor d = new Doctor();
        d.setId("doc-1");
        d.setEmail(email);
        Bookings bookings = new Bookings();
        bookings.setMaxCount(5);
        bookings.setRate(300.0);
        bookings.setBookingListMap(slots);
        d.setBookings(bookings);
        return d;
    }

    // --- refreshBookingSchedules ---

    @Test
    void refreshBookingSchedules_addsNewDayAndRemovesPastSlots() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Map<LocalDate, BookingList> slots = new HashMap<>();
        slots.put(yesterday, BookingList.builder().availibility(Availibility.AVAILABLE).build());
        slots.put(today, BookingList.builder().availibility(Availibility.AVAILABLE).build());

        Doctor doctor = buildDoctorWithSlots("doctor@example.com", slots);
        when(doctorRepository.findAll()).thenReturn(List.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        doctorService.refreshBookingSchedules();

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(captor.capture());

        Map<LocalDate, BookingList> saved = captor.getValue().getBookings().getBookingListMap();
        assertThat(saved).doesNotContainKey(yesterday);
        assertThat(saved).containsKey(today);
        for (int i = 0; i < 31; i++) {
            assertThat(saved).containsKey(today.plusDays(i));
        }
    }

    @Test
    void refreshBookingSchedules_doesNotOverwriteExistingSlots() {
        LocalDate today = LocalDate.now();

        Map<LocalDate, BookingList> slots = new HashMap<>();
        for (int i = 0; i < 31; i++) {
            slots.put(today.plusDays(i), BookingList.builder().availibility(Availibility.BOOKED).build());
        }

        Doctor doctor = buildDoctorWithSlots("doctor@example.com", slots);
        when(doctorRepository.findAll()).thenReturn(List.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        doctorService.refreshBookingSchedules();

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(captor.capture());

        Map<LocalDate, BookingList> saved = captor.getValue().getBookings().getBookingListMap();
        for (int i = 0; i < 31; i++) {
            assertThat(saved.get(today.plusDays(i)).getAvailibility()).isEqualTo(Availibility.BOOKED);
        }
    }

    @Test
    void refreshBookingSchedules_noDoctors_doesNothing() {
        when(doctorRepository.findAll()).thenReturn(List.of());

        doctorService.refreshBookingSchedules();

        verify(doctorRepository, never()).save(any());
    }

    @Test
    void refreshBookingSchedules_multipleDoctors_allUpdated() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Doctor d1 = buildDoctorWithSlots("doc1@example.com", new HashMap<>(Map.of(
                yesterday, BookingList.builder().availibility(Availibility.AVAILABLE).build())));
        Doctor d2 = buildDoctorWithSlots("doc2@example.com", new HashMap<>(Map.of(
                yesterday, BookingList.builder().availibility(Availibility.AVAILABLE).build())));

        when(doctorRepository.findAll()).thenReturn(List.of(d1, d2));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        doctorService.refreshBookingSchedules();

        verify(doctorRepository, times(2)).save(any(Doctor.class));
    }

    @Test
    void refreshBookingSchedules_emptySlots_addsAll31Days() {
        LocalDate today = LocalDate.now();

        Doctor doctor = buildDoctorWithSlots("doctor@example.com", new HashMap<>());
        when(doctorRepository.findAll()).thenReturn(List.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        doctorService.refreshBookingSchedules();

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(captor.capture());

        Map<LocalDate, BookingList> saved = captor.getValue().getBookings().getBookingListMap();
        assertThat(saved).hasSize(31);
        for (int i = 0; i < 31; i++) {
            assertThat(saved.get(today.plusDays(i)).getAvailibility()).isEqualTo(Availibility.AVAILABLE);
        }
    }
}
