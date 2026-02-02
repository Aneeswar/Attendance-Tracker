package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.AttendanceInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceInputRepository extends JpaRepository<AttendanceInput, Long> {
    Optional<AttendanceInput> findByCourseId(Long courseId);
    List<AttendanceInput> findAllByCourseId(Long courseId);
    Optional<AttendanceInput> findFirstByCourseIdOrderByLastUpdatedDesc(Long courseId);
}
