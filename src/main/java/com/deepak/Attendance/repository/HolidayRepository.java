package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Optional<Holiday> findByDate(LocalDate date);
    Optional<Holiday> findByDateAndSemesterId(LocalDate date, Long semesterId);
    boolean existsByDateAndSemesterId(LocalDate date, Long semesterId);
    List<Holiday> findAllByOrderByDateAsc();
    List<Holiday> findAllByDate(LocalDate date);
    List<Holiday> findBySemesterIsNull();
    long countBySemesterId(Long semesterId);
    List<Holiday> findByAcademicCalendarId(Long academicCalendarId);
    List<Holiday> findBySemesterIdOrderByDateAsc(Long semesterId);
}
