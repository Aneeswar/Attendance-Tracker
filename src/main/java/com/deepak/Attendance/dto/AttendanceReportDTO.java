package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportDTO {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Double currentPercentage;
    private Integer futureClassesAvailable;
    private Integer minimumClassesToAttend;
    private String status; // SAFE, AT_RISK, IMPOSSIBLE
    
    // Classes that can be skipped while maintaining threshold
    private Integer classesCanSkip75; // For 75% threshold
    private Integer classesCanSkip65; // For 65% threshold (medical/other)
    
    // Current attendance data
    private Integer totalClassesConducted;
    private Integer classesAttended;
    
    // Upcoming exam information (only the next upcoming exam)
    private String upcomingExamName; // CAT-1, CAT-2, or FAT
    private LocalDate upcomingExamStartDate;
    private LocalDate upcomingExamEndDate;
    private boolean upcomingExamEligible; // For 75% (can achieve if attend required classes)
    private boolean upcomingExamEligibleRelaxed; // For 65% (can achieve if attend required classes)
    private Double projectedAttendancePercentage; // Projected % if all remaining classes are attended
    
    // Constructor for basic report (backward compatibility)
    public AttendanceReportDTO(String courseCode, String courseName, Double currentPercentage, 
                               Integer futureClassesAvailable, Integer minimumClassesToAttend, String status) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.currentPercentage = currentPercentage;
        this.futureClassesAvailable = futureClassesAvailable;
        this.minimumClassesToAttend = minimumClassesToAttend;
        this.status = status;
    }
}
