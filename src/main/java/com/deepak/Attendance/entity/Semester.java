package com.deepak.Attendance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "semesters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String semesterName;

    @Column(nullable = false)
    private String academicYear;

    @Column(nullable = false)
    private LocalDate semesterStartDate;

    @Column(nullable = false)
    private LocalDate semesterEndDate;

    @Column
    private LocalDate cat1StartDate;

    @Column
    private LocalDate cat1EndDate;

    @Column
    private LocalDate cat2StartDate;

    @Column
    private LocalDate cat2EndDate;

    @Column
    private LocalDate fatStartDate;

    @Column
    private LocalDate fatEndDate;

    @Column
    private LocalDate labFatStartDate;

    @Column
    private LocalDate labFatEndDate;

    @Column(nullable = false)
    private Boolean active = true;

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