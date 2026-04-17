package com.deepak.Attendance.dto.ocr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrExtractResponseDTO {
    private String status;
    private Integer count;
    private List<OcrExtractRowDTO> data = new ArrayList<>();
}
