package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.AttendanceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceReportRepository extends JpaRepository<AttendanceReport, Long> {
    
    Optional<AttendanceReport> findByCourseId(Long courseId);
    
    List<AttendanceReport> findByCourse_UserId(Long userId);
    
    void deleteByCourseId(Long courseId);
}
