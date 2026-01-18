package com.project.doctorService.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Bookings {
    private int maxCount;
    private int rate;
    private Map<LocalDate,BookingList> bookingListMap;
}