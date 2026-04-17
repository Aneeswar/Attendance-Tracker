package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateRangeHolidayRequest {
    private Long semesterId;
    private List<Long> targetSemesterIds;
    private String startDate;  // Format: yyyy-MM-dd
    private String endDate;    // Format: yyyy-MM-dd
    private String name;       // Holiday name (e.g., "Winter Break")
    private String type;       // Holiday type (PUBLIC, ACADEMIC, RESTRICTED)
    private String scope;      // FULL, MORNING, AFTERNOON
}
