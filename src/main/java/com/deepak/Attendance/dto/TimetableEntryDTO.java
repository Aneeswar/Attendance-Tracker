package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntryDTO {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Map<String, Integer> weeklySchedule; // Day -> Classes count
    private LocalDate courseStartDate; // Start date of the course for date-based attendance
}
