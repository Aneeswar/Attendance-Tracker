package com.deepak.Attendance.dto;

import com.deepak.Attendance.entity.enums.DayOfWeek;
import com.deepak.Attendance.entity.enums.SessionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedScheduleRowDTO {
    private String slotName;
    private DayOfWeek dayOfWeek;
    private SessionType sessionType;
    private LocalTime startTime;
    private LocalTime endTime;
}
