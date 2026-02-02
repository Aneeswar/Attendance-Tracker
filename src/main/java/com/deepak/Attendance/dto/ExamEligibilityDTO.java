package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamEligibilityDTO {
    private String courseCode;
    private String courseName;
    private Double currentPercentage;
    
    // CAT-1 Eligibility
    private boolean cat1Eligible;
    private boolean cat1Upcoming; // true if exam hasn't started yet
    private LocalDate cat1StartDate;
    private LocalDate cat1EndDate;
    
    // CAT-2 Eligibility
    private boolean cat2Eligible;
    private boolean cat2Upcoming;
    private LocalDate cat2StartDate;
    private LocalDate cat2EndDate;
    
    // FAT Eligibility
    private boolean fatEligible;
    private boolean fatUpcoming;
    private LocalDate fatStartDate;
    private LocalDate fatEndDate;
    
    // Relaxation info (for medical/other cases)
    private boolean withRelaxation;
    private Double requiredPercentage; // 75% or 65%
}
