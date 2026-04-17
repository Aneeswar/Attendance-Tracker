package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    List<Semester> findAllByOrderBySemesterStartDateDesc();

    List<Semester> findByActiveTrueOrderBySemesterStartDateDesc();

    Optional<Semester> findFirstByActiveTrueOrderBySemesterStartDateDesc();

    Optional<Semester> findFirstBySemesterNameIgnoreCaseOrderByIdAsc(String semesterName);

    List<Semester> findBySemesterStartDateLessThanEqualAndSemesterEndDateGreaterThanEqual(LocalDate date1, LocalDate date2);
}