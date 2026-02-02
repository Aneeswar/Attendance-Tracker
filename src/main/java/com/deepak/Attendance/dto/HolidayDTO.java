package com.deepak.Attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDTO {
    private Long id;
    private LocalDate date;
    private String reason;
    private String type;
    private LocalDate createdAt;

    // Alias for frontend compatibility
    @JsonProperty("name")
    public String getName() {
        return reason;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.reason = name;
    }
}
