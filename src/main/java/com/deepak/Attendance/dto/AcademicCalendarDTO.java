package com.deepak.Attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicCalendarDTO {
    private Long id;
    private String academicYear;
    private LocalDate semesterStartDate;
    private LocalDate examStartDate;
    
    // CAT-1 Exam Date Range
    private LocalDate cat1StartDate;
    private LocalDate cat1EndDate;
    
    // CAT-2 Exam Date Range
    private LocalDate cat2StartDate;
    private LocalDate cat2EndDate;
    
    // FAT Exam Date Range
    private LocalDate fatStartDate;
    private LocalDate fatEndDate;
    
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // Alias for frontend compatibility - semesterEndDate maps to examStartDate
    @JsonProperty("semesterEndDate")
    public LocalDate getSemesterEndDate() {
        return examStartDate;
    }

    @JsonProperty("semesterEndDate")
    public void setSemesterEndDate(LocalDate semesterEndDate) {
        this.examStartDate = semesterEndDate;
    }
}
