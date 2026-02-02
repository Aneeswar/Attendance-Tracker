package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.AttendanceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceResultRepository extends JpaRepository<AttendanceResult, Long> {
    List<AttendanceResult> findByCourseId(Long courseId);
    List<AttendanceResult> findByCourseIdOrderByCalculatedAtDesc(Long courseId);
}
