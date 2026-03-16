package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.deepak.Attendance.entity.TimetableEntry.Session;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyScheduleItemDTO {
    private String dayOfWeek;
    private Session session;
    private Integer classesCount = 1;
}