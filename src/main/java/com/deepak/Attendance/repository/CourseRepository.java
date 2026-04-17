package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserId(Long userId);
    List<Course> findByUserIdAndSemesterId(Long userId, Long semesterId);
    List<Course> findByUserIdAndSemesterIsNull(Long userId);
    List<Course> findBySemesterIsNull();
    List<Course> findBySemesterId(Long semesterId);
    long countBySemesterId(Long semesterId);
    Page<Course> findBySemesterId(Long semesterId, Pageable pageable);
    long countByUserIdAndSemesterId(Long userId, Long semesterId);
    Page<Course> findByCourseCatalog_CourseCodeContainingIgnoreCaseOrCourseCatalog_CourseNameContainingIgnoreCase(String courseCode, String courseName, Pageable pageable);
    Page<Course> findBySemesterIdAndCourseCatalog_CourseCodeContainingIgnoreCaseOrSemesterIdAndCourseCatalog_CourseNameContainingIgnoreCase(Long semesterId1, String courseCode, Long semesterId2, String courseName, Pageable pageable);
    Page<Course> findByCourseCodeContainingIgnoreCaseOrCourseNameContainingIgnoreCase(String courseCode, String courseName, Pageable pageable);
    Optional<Course> findByUserIdAndCourseCatalog_CourseCodeIgnoreCase(Long userId, String courseCode);
    Optional<Course> findByUserIdAndSemesterIdAndCourseCatalog_CourseCodeIgnoreCase(Long userId, Long semesterId, String courseCode);
    Optional<Course> findByUserIdAndSemesterIsNullAndCourseCatalog_CourseCodeIgnoreCase(Long userId, String courseCode);
    Optional<Course> findByUserIdAndCourseCode(Long userId, String courseCode);
    boolean existsByUserIdAndCourseCatalog_CourseCodeIgnoreCase(Long userId, String courseCode);
    boolean existsByUserIdAndCourseCode(Long userId, String courseCode);
    long countByUserId(Long userId);

    @Query("SELECT c FROM Course c WHERE c.userId = :userId AND ((:semesterId IS NOT NULL AND c.semester.id = :semesterId) OR (:semesterId IS NULL AND c.semester IS NULL))")
    List<Course> findByUserIdWithSemesterContext(@Param("userId") Long userId, @Param("semesterId") Long semesterId);
}
