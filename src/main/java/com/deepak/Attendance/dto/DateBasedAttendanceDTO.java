package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateBasedAttendanceDTO {
    private LocalDate date;
    private Boolean attended;
    private String dayOfWeek;
}
