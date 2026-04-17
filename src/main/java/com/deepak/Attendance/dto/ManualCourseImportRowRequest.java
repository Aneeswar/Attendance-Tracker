package com.deepak.Attendance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ManualCourseImportRowRequest {
    private String courseCode;
    private String courseName;
    private String slotString;
    private LocalDate registeredDate;
    private String courseType;
    private Map<String, Double> fieldConfidence = new HashMap<>();
    private Map<String, List<String>> suggestions = new HashMap<>();

    public ManualCourseImportRowRequest(String courseCode,
                                        String courseName,
                                        String slotString,
                                        LocalDate registeredDate,
                                        String courseType) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.slotString = slotString;
        this.registeredDate = registeredDate;
        this.courseType = courseType;
    }
}
