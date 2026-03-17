package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.CourseCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseCatalogRepository extends JpaRepository<CourseCatalog, Long> {
    Optional<CourseCatalog> findByCourseCodeAndCourseName(String courseCode, String courseName);
}
