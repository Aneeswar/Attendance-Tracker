package com.deepak.Attendance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_input")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Integer totalClassesConducted; // T

    @Column(nullable = false)
    private Integer classesAttended; // A

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime lastUpdated;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
