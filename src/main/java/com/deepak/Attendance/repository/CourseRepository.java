package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserId(Long userId);
    Page<Course> findByCourseCatalog_CourseCodeContainingIgnoreCaseOrCourseCatalog_CourseNameContainingIgnoreCase(String courseCode, String courseName, Pageable pageable);
    Page<Course> findByCourseCodeContainingIgnoreCaseOrCourseNameContainingIgnoreCase(String courseCode, String courseName, Pageable pageable);
    Optional<Course> findByUserIdAndCourseCatalog_CourseCodeIgnoreCase(Long userId, String courseCode);
    Optional<Course> findByUserIdAndCourseCode(Long userId, String courseCode);
    boolean existsByUserIdAndCourseCatalog_CourseCodeIgnoreCase(Long userId, String courseCode);
    boolean existsByUserIdAndCourseCode(Long userId, String courseCode);
    long countByUserId(Long userId);
}
