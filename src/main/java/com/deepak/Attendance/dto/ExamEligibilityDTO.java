package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamEligibilityDTO {
    private String examName;
    private LocalDate examStartDate;
    private LocalDate examEndDate;
    private LocalDate attendanceCutoffDate;
    
    private boolean eligible75;
    private boolean eligible65;
    
    private Integer futureClassesUntilExam;
    private Integer totalClassesConductedUntilCutoff;
    private Integer classesAttendedUntilCutoff;
    private Integer minimumClassesToAttend75;
    private Integer minimumClassesToAttend65;
    private Integer classesCanSkip75;
    private Integer classesCanSkip65;
    
    private Double projectedPercentage;
    private Double targetPercentage75;
    private Double targetPercentage65;
    private java.util.List<LocalDate> futureClassDates;
    
    private boolean isAvailable; // Whether the exam dates are set and calculation is possible
    private boolean isUpcoming; // Has it started yet?
    private boolean isOngoing; // Is it currently happening?
    private boolean isCompleted; // Is it finished?
}
