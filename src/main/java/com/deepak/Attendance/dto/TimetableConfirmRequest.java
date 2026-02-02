package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableConfirmRequest {
    private String courseCode;
    private String courseName;
    private Map<String, Integer> weeklySchedule;
    private LocalDate courseStartDate; // Optional: start date for date-based attendance
}

