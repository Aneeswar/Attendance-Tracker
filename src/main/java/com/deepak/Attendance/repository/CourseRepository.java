package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserId(Long userId);
    Optional<Course> findByUserIdAndCourseCode(Long userId, String courseCode);
    boolean existsByUserIdAndCourseCode(Long userId, String courseCode);
}
