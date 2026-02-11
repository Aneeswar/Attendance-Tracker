package com.deepak.Attendance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_report", indexes = {
    @Index(name = "idx_course_id", columnList = "course_id"),
    @Index(name = "idx_calculated_at", columnList = "calculatedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // Current attendance data
    @Column(nullable = false)
    private Integer totalClassesConducted;

    @Column(nullable = false)
    private Integer classesAttended;

    @Column(nullable = false)
    private Double currentPercentage;

    // Future classes info
    @Column(nullable = false)
    private Integer futureClassesAvailable;

    // 75% threshold
    @Column(nullable = false)
    private Integer minimumClassesToAttend75;

    @Column(nullable = false)
    private Integer classesCanSkip75;

    // 65% threshold (medical/other relaxation)
    @Column(nullable = false)
    private Integer minimumClassesToAttend65;

    @Column(nullable = false)
    private Integer classesCanSkip65;

    // Overall status (based on full semester)
    @Column(nullable = false)
    private String status; // SAFE, AT_RISK, IMPOSSIBLE

    // Upcoming exam info
    @Column(length = 50)
    private String upcomingExamName; // CAT-1, CAT-2, FAT

    @Column
    private LocalDate upcomingExamStartDate;

    @Column
    private LocalDate upcomingExamEndDate;

    @Column(nullable = false)
    private Boolean upcomingExamEligible75;

    @Column(nullable = false)
    private Boolean upcomingExamEligible65;

    @Column
    private Integer futureClassesUntilExam;

    @Column
    private Integer minimumClassesToAttendForExam75;

    @Column
    private Integer minimumClassesToAttendForExam65;

    @Column
    private Integer classesCanSkipUntilExam75;

    @Column
    private Integer classesCanSkipUntilExam65;

    @Column
    private Double projectedAttendancePercentage;

    @Column(nullable = false)
    private Boolean isStale = false;

    // Metadata
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime calculatedAt;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        calculatedAt = LocalDateTime.now();
    }
}
