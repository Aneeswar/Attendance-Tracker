package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findByCourseId(Long courseId);
    List<TimetableEntry> findByCourseIdAndDayOfWeek(Long courseId, String dayOfWeek);
}
