package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseAttendanceCalendarDTO {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> classScheduleDays; // e.g., ["MONDAY", "TUESDAY", "THURSDAY"]
    private List<DateBasedAttendanceDTO> attendanceDates;
    private Integer totalDaysAvailable;
    private Integer totalDaysAttended;
}
