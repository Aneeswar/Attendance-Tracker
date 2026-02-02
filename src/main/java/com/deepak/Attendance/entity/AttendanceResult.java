package com.deepak.Attendance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Double currentPercentage;

    @Column(nullable = false)
    private Integer futureClassesAvailable;

    @Column(nullable = false)
    private Integer minClassesRequired;

    @Column(nullable = false)
    private String eligibilityStatus; // SAFE, AT_RISK, IMPOSSIBLE

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }
}
