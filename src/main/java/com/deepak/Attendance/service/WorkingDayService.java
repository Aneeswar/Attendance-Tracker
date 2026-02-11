package com.deepak.Attendance.service;

import com.deepak.Attendance.entity.Holiday;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkingDayService {

    @Autowired
    private AcademicCalendarRepository academicCalendarRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    /**
     * Determines if a given date is a valid working day
     * A valid working day:
     * - Is between semester start and exam start (inclusive)
     * - Is Tuesday to Saturday only
     * - Is NOT in the holidays table
     *
     * @param date The date to check
     * @return true if it's a valid working day, false otherwise
     */
    public boolean isWorkingDay(LocalDate date) {
        var calendar = academicCalendarRepository.findAll().stream().findFirst();
        if (calendar.isEmpty()) {
            return false;
        }

        LocalDate semesterStart = calendar.get().getSemesterStartDate();
        LocalDate examStart = calendar.get().getExamStartDate();

        // Check if date is within academic period
        // User requested that the exam start date (semester end date) be considered a working day
        if (date.isBefore(semesterStart) || date.isAfter(examStart)) {
            return false;
        }

        // Check if it's a valid day of week (Tuesday to Saturday)
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SUNDAY || dayOfWeek == DayOfWeek.MONDAY) {
            return false;
        }

        // Check if it's a holiday
        if (holidayRepository.findByDate(date).isPresent()) {
            return false;
        }

        return true;
    }

    /**
     * Gets all working days within the current semester
     *
     * @return List of all working days
     */
    public List<LocalDate> getAllWorkingDays() {
        var calendar = academicCalendarRepository.findAll().stream().findFirst();
        if (calendar.isEmpty()) {
            return List.of();
        }

        LocalDate semesterStart = calendar.get().getSemesterStartDate();
        LocalDate examStart = calendar.get().getExamStartDate();

        Set<LocalDate> holidays = holidayRepository.findAll()
                .stream()
                .map(Holiday::getDate)
                .collect(Collectors.toSet());

        // Use plusDays(1) to include the examStart day itself
        return semesterStart.datesUntil(examStart.plusDays(1))
                .filter(date -> isValidWorkingDayInternal(date, semesterStart, examStart, holidays))
                .collect(Collectors.toList());
    }

    /**
     * Gets the last working day before exam starts
     *
     * @return The last working day, or empty if no working days exist
     */
    public LocalDate getLastWorkingDay() {
        var calendar = academicCalendarRepository.findAll().stream().findFirst();
        if (calendar.isEmpty()) {
            return null;
        }

        LocalDate examStart = calendar.get().getExamStartDate();
        // User requested that the exam start date (semester end date) be considered the last working day
        LocalDate current = examStart;

        while (!current.isBefore(calendar.get().getSemesterStartDate())) {
            if (isWorkingDay(current)) {
                return current;
            }
            current = current.minusDays(1);
        }

        return null;
    }

    /**
     * Gets the number of working days between two dates (inclusive)
     *
     * @param startDate The start date
     * @param endDate   The end date
     * @return The count of working days
     */
    public long countWorkingDays(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .filter(this::isWorkingDay)
                .count();
    }

    /**
     * Gets the next working day from a given date
     *
     * @param fromDate The reference date
     * @return The next working day, or null if none exists
     */
    public LocalDate getNextWorkingDay(LocalDate fromDate) {
        var calendar = academicCalendarRepository.findAll().stream().findFirst();
        if (calendar.isEmpty()) {
            return null;
        }

        LocalDate nextDay = fromDate.plusDays(1);
        LocalDate examStart = calendar.get().getExamStartDate();

        while (nextDay.isBefore(examStart)) {
            if (isWorkingDay(nextDay)) {
                return nextDay;
            }
            nextDay = nextDay.plusDays(1);
        }

        return null;
    }

    private boolean isValidWorkingDayInternal(LocalDate date, LocalDate semesterStart, LocalDate examStart, Set<LocalDate> holidays) {

        // Check if it's a valid day of week (Tuesday to Saturday)
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SUNDAY || dayOfWeek == DayOfWeek.MONDAY) {
            return false;
        }

        // Check if it's a holiday
        return !holidays.contains(date);
    }
}
