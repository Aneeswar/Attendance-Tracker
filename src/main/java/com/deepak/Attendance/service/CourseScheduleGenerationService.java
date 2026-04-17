package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.ResolvedScheduleRowDTO;
import com.deepak.Attendance.entity.Course;
import com.deepak.Attendance.entity.CourseSchedule;
import com.deepak.Attendance.entity.TimetableEntry;
import com.deepak.Attendance.entity.enums.DayOfWeek;
import com.deepak.Attendance.repository.CourseScheduleRepository;
import com.deepak.Attendance.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseScheduleGenerationService {

    private final CourseScheduleRepository courseScheduleRepository;
    private final TimetableEntryRepository timetableEntryRepository;

    public CourseScheduleGenerationService(CourseScheduleRepository courseScheduleRepository,
                                           TimetableEntryRepository timetableEntryRepository) {
        this.courseScheduleRepository = courseScheduleRepository;
        this.timetableEntryRepository = timetableEntryRepository;
    }

    public void replaceGeneratedSchedules(Course course, List<ResolvedScheduleRowDTO> resolvedRows) {
        courseScheduleRepository.deleteByCourseId(course.getId());
        timetableEntryRepository.deleteByCourseId(course.getId());

        List<CourseSchedule> schedules = toCourseSchedules(course, resolvedRows);
        if (!schedules.isEmpty()) {
            courseScheduleRepository.saveAll(schedules);
        }

        List<TimetableEntry> summaryRows = toTimetableSummary(course, resolvedRows);
        if (!summaryRows.isEmpty()) {
            timetableEntryRepository.saveAll(summaryRows);
        }
    }

    private List<CourseSchedule> toCourseSchedules(Course course, List<ResolvedScheduleRowDTO> resolvedRows) {
        LinkedHashSet<String> dedupe = new LinkedHashSet<>();
        List<CourseSchedule> rows = new ArrayList<>();

        for (ResolvedScheduleRowDTO row : resolvedRows) {
            String key = row.getSlotName() + "|" + row.getDayOfWeek() + "|" + row.getSessionType() + "|" + row.getStartTime() + "|" + row.getEndTime();
            if (!dedupe.add(key)) {
                continue;
            }

            CourseSchedule schedule = new CourseSchedule();
            schedule.setCourse(course);
            schedule.setSlotName(row.getSlotName());
            schedule.setDayOfWeek(row.getDayOfWeek());
            schedule.setSessionType(row.getSessionType());
            schedule.setStartTime(row.getStartTime());
            schedule.setEndTime(row.getEndTime());
            rows.add(schedule);
        }

        return rows;
    }

    private List<TimetableEntry> toTimetableSummary(Course course, List<ResolvedScheduleRowDTO> resolvedRows) {
        Map<String, Long> counts = resolvedRows.stream().collect(Collectors.groupingBy(
                row -> toJavaDayName(row.getDayOfWeek()) + "|" + toSession(row.getStartTime()),
                Collectors.counting()
        ));

        List<TimetableEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            TimetableEntry timetableEntry = new TimetableEntry();
            timetableEntry.setCourse(course);
            timetableEntry.setDayOfWeek(parts[0]);
            timetableEntry.setSession(TimetableEntry.Session.valueOf(parts[1]));
            timetableEntry.setClassesCount(entry.getValue().intValue());
            entries.add(timetableEntry);
        }

        return entries;
    }

    private TimetableEntry.Session toSession(LocalTime startTime) {
        return startTime.isBefore(LocalTime.of(14, 0))
                ? TimetableEntry.Session.MORNING
                : TimetableEntry.Session.AFTERNOON;
    }

    private String toJavaDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case TUE -> "TUESDAY";
            case WED -> "WEDNESDAY";
            case THU -> "THURSDAY";
            case FRI -> "FRIDAY";
            case SAT -> "SATURDAY";
        };
    }
}
