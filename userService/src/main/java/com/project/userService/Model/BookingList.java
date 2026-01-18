package com.project.userService.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class BookingList {
    private Availibility availibility;
    @Builder.Default
    List<String> bookingId=new ArrayList<>();
}
