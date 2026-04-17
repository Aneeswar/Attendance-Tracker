package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.MasterTimetableSlot;
import com.deepak.Attendance.entity.enums.DayOfWeek;
import com.deepak.Attendance.entity.enums.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MasterTimetableSlotRepository extends JpaRepository<MasterTimetableSlot, Long> {

    List<MasterTimetableSlot> findBySlotNameIgnoreCase(String slotName);

    @Query("SELECT m FROM MasterTimetableSlot m WHERE UPPER(m.slotName) IN :slotNames")
    List<MasterTimetableSlot> findBySlotNamesUpperIn(@Param("slotNames") Collection<String> slotNames);

    boolean existsBySlotNameAndDayOfWeekAndSessionTypeAndStartTimeAndEndTime(
            String slotName,
            DayOfWeek dayOfWeek,
            SessionType sessionType,
            LocalTime startTime,
            LocalTime endTime
    );
}
