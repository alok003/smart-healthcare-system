package com.project.appointmentService.Model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class HealthCheck {
    @NotNull(message = "Height is required")
    @Min(value = 50, message = "Height must be at least 50 cm")
    @Max(value = 300, message = "Height must be less than 300 cm")
    private Integer height;
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "2.0", message = "Weight must be at least 2 kg")
    @DecimalMax(value = "500.0", message = "Weight must be less than 500 kg")
    private Float weight;
    @NotNull(message = "Systolic BP is required")
    @Min(value = 70, message = "Systolic BP too low")
    @Max(value = 250, message = "Systolic BP too high")
    private Integer bpSys;
    @NotNull(message = "Diastolic BP is required")
    @Min(value = 40, message = "Diastolic BP too low")
    @Max(value = 150, message = "Diastolic BP too high")
    private Integer bpDia;
    @NotNull(message = "Oxygen level is required")
    @Min(value = 50, message = "Oxygen level too low")
    @Max(value = 100, message = "Oxygen level cannot exceed 100")
    private Integer oxyLvl;
    @NotNull(message = "Blood sugar level is required")
    @Min(value = 40, message = "Blood sugar too low")
    @Max(value = 600, message = "Blood sugar too high")
    private Integer bloodSugar;
    @NotNull(message = "Heart rate is required")
    @Min(value = 30, message = "Heart rate too low")
    @Max(value = 220, message = "Heart rate too high")
    private Integer heartRate;
    @NotNull(message = "Body temperature is required")
    @DecimalMin(value = "30.0", message = "Temperature too low")
    @DecimalMax(value = "45.0", message = "Temperature too high")
    private Float bodyTemperature;
    @Min(value = 5, message = "Respiratory rate too low")
    @Max(value = 60, message = "Respiratory rate too high")
    private Integer respiratoryRate;
    private Float bmi;
}
