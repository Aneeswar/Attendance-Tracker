package com.deepak.Attendance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // Foreign key to User (student)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_catalog_id")
    private CourseCatalog courseCatalog;

    @Column(nullable = false)
    private String courseCode;

    @Column
    private String courseName;

    @Column
    private String slot; // Added slot to the course level

    @Column
    private LocalDate courseStartDate; // Start date of the course for date-based attendance

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimetableEntry> timetableEntries;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttendanceInput> attendanceInputs;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttendanceResult> attendanceResults;

    public String getCourseCode() {
        if (courseCatalog != null && courseCatalog.getCourseCode() != null) {
            return courseCatalog.getCourseCode();
        }
        return courseCode;
    }

    public String getCourseName() {
        if (courseCatalog != null && courseCatalog.getCourseName() != null) {
            return courseCatalog.getCourseName();
        }
        return courseName;
    }
}
