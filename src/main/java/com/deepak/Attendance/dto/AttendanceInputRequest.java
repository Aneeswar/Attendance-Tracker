package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceInputRequest {
    private String courseCode;
    private Integer totalClassesConducted;
    private Integer classesAttended;
}
