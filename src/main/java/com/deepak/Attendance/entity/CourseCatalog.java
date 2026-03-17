package com.deepak.Attendance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "course_catalog",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"courseCode", "courseName"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private String courseName;
}
