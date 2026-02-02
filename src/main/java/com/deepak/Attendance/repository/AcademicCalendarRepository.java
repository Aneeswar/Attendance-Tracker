package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.AcademicCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AcademicCalendarRepository extends JpaRepository<AcademicCalendar, Long> {
    Optional<AcademicCalendar> findBySemesterStartDateLessThanEqualAndExamStartDateGreaterThanEqual(LocalDate today, LocalDate today1);
}
