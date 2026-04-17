package com.deepak.Attendance.dto.ocr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OcrExtractRowDTO {

    @JsonProperty("course_code")
    private String courseCode;

    @JsonProperty("course_name")
    private String courseName;

    @JsonProperty("slot")
    private String slot;

    @JsonProperty("registered_date")
    private String registeredDate;
}
