package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualCourseImportPreviewResponse {
    private boolean valid;
    private String semesterName;
    private Long semesterId;
    private List<ManualCourseImportPreviewRowDTO> rows = new ArrayList<>();
}
