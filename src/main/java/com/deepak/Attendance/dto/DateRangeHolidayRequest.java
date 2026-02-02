package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateRangeHolidayRequest {
    private String startDate;  // Format: yyyy-MM-dd
    private String endDate;    // Format: yyyy-MM-dd
    private String name;       // Holiday name (e.g., "Winter Break")
    private String type;       // Holiday type (PUBLIC, ACADEMIC, RESTRICTED)
}
