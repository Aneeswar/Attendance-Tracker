package com.deepak.Attendance.dto;

import com.deepak.Attendance.entity.Holiday;
import com.deepak.Attendance.entity.StudentHolidayRequest;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudentHolidayRequestDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long semesterId;
    private String semesterName;
    private LocalDate holidayDate;
    private String reason;
    private Holiday.HolidayScope scope;
    private StudentHolidayRequest.RequestStatus status;
    private String adminComment;
    private LocalDateTime createdAt;
}
