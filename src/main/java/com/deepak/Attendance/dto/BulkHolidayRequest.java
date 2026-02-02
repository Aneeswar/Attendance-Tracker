package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkHolidayRequest {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Holiday {
        private LocalDate date;
        private String reason;
    }
    
    private List<Holiday> holidays;
}
