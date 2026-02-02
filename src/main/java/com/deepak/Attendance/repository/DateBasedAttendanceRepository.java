package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.DateBasedAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DateBasedAttendanceRepository extends JpaRepository<DateBasedAttendance, Long> {

    List<DateBasedAttendance> findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long courseId, LocalDate startDate, LocalDate endDate);

    Optional<DateBasedAttendance> findByCourseIdAndAttendanceDate(Long courseId, LocalDate attendanceDate);

    @Query("SELECT COUNT(d) FROM DateBasedAttendance d WHERE d.course.id = :courseId AND d.attended = true AND d.attendanceDate BETWEEN :startDate AND :endDate")
    Integer countAttendedDays(@Param("courseId") Long courseId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(d) FROM DateBasedAttendance d WHERE d.course.id = :courseId AND d.attendanceDate BETWEEN :startDate AND :endDate")
    Integer countTotalDays(@Param("courseId") Long courseId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    void deleteByCourseId(Long courseId);
}
