package com.deepak.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkHolidayRequest {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Holiday {
        private String date; // Changed to String to handle incoming JSON
        private String reason;
        private String type; // New field
        private String scope; // New field
    }
    
    private List<Holiday> holidays;
}
