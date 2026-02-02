package com.deepak.Attendance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "academic_calendar")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String academicYear;

    @Column(nullable = false)
    private LocalDate semesterStartDate;

    @Column(nullable = false)
    private LocalDate examStartDate;

    // CAT-1 Exam Date Range
    @Column
    private LocalDate cat1StartDate;

    @Column
    private LocalDate cat1EndDate;

    // CAT-2 Exam Date Range
    @Column
    private LocalDate cat2StartDate;

    @Column
    private LocalDate cat2EndDate;

    // FAT Exam Date Range
    @Column
    private LocalDate fatStartDate;

    @Column
    private LocalDate fatEndDate;

    @Column(nullable = false)
    private LocalDate createdAt;

    @Column(nullable = false)
    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDate.now();
    }
}
