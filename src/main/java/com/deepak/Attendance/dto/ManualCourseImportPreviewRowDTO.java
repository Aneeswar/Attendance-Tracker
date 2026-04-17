package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualCourseImportPreviewRowDTO {
    private int rowNumber;
    private String courseCode;
    private String courseName;
    private String slotString;
    private String courseType;
    private LocalDate registeredDate;
    private LocalDate effectiveStartDate;
    private List<String> normalizedTokens = new ArrayList<>();
    private List<ResolvedScheduleRowDTO> resolvedSchedule = new ArrayList<>();
    private Map<String, Double> fieldConfidence = new HashMap<>();
    private Map<String, List<String>> suggestions = new HashMap<>();
    private List<String> errors = new ArrayList<>();

    public boolean isValid() {
        return errors == null || errors.isEmpty();
    }
}
