package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualCourseImportConfirmResponse {
    private String message;
    private String semesterName;
    private Long semesterId;
    private int processedRows;
    private int createdCount;
    private int updatedCount;
    private List<String> createdCourseCodes = new ArrayList<>();
    private List<String> updatedCourseCodes = new ArrayList<>();
}
