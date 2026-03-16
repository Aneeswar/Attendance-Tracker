package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntryDTO {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String slot; // Added slot for the course
    private List<WeeklyScheduleItemDTO> weeklySchedule;
    private LocalDate courseStartDate; // Start date of the course for date-based attendance
}
