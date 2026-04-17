package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.AcademicCalendarDTO;
import com.deepak.Attendance.entity.AcademicCalendar;
import com.deepak.Attendance.entity.Semester;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import com.deepak.Attendance.repository.SemesterRepository;
import com.deepak.Attendance.repository.StudentHolidayRequestRepository;
import com.deepak.Attendance.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AcademicCalendarService {

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private AcademicCalendarRepository academicCalendarRepository;

    @Autowired
    private ObjectProvider<StudentService> studentServiceProvider;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private StudentHolidayRequestRepository studentHolidayRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public AcademicCalendarDTO saveOrUpdateAcademicCalendar(AcademicCalendarDTO dto) {
        if (dto.getSemesterStartDate() != null && dto.getExamStartDate() != null
                && dto.getSemesterStartDate().isAfter(dto.getExamStartDate())) {
            throw new IllegalArgumentException("Semester start date must be before semester end date");
        }

        if (semesterRepository == null) {
            academicCalendarRepository.deleteAll();
            AcademicCalendar calendar = new AcademicCalendar();
            calendar.setAcademicYear(dto.getAcademicYear());
            calendar.setSemesterStartDate(dto.getSemesterStartDate());
            calendar.setExamStartDate(dto.getExamStartDate());
            calendar.setCat1StartDate(dto.getCat1StartDate());
            calendar.setCat1EndDate(dto.getCat1EndDate());
            calendar.setCat2StartDate(dto.getCat2StartDate());
            calendar.setCat2EndDate(dto.getCat2EndDate());
            calendar.setFatStartDate(dto.getFatStartDate());
            calendar.setFatEndDate(dto.getFatEndDate());
            AcademicCalendar savedCalendar = academicCalendarRepository.save(calendar);
            AcademicCalendarDTO result = new AcademicCalendarDTO();
            result.setId(savedCalendar.getId());
            result.setAcademicYear(savedCalendar.getAcademicYear());
            result.setSemesterStartDate(savedCalendar.getSemesterStartDate());
            result.setExamStartDate(savedCalendar.getExamStartDate());
            result.setCat1StartDate(savedCalendar.getCat1StartDate());
            result.setCat1EndDate(savedCalendar.getCat1EndDate());
            result.setCat2StartDate(savedCalendar.getCat2StartDate());
            result.setCat2EndDate(savedCalendar.getCat2EndDate());
            result.setFatStartDate(savedCalendar.getFatStartDate());
            result.setFatEndDate(savedCalendar.getFatEndDate());
            return result;
        }

        Semester semester;
        if (dto.getId() != null) {
            semester = semesterRepository.findById(dto.getId()).orElse(new Semester());
        } else {
            semester = semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc().orElse(new Semester());
        }

        if (semester.getId() == null) {
            semester.setActive(dto.getActive() == null || dto.getActive());
        }

        applyDto(semester, dto);
        Semester saved = semesterRepository.save(semester);

        studentServiceProvider.ifAvailable(studentService -> {
            try {
                studentService.markAllAttendanceReportsAsStale();
            } catch (Exception e) {
                log.error("Error marking reports stale after semester update", e);
            }
        });

        return convertToDTO(saved);
    }

    @Transactional
    public AcademicCalendarDTO createSemester(AcademicCalendarDTO dto) {
        Semester semester = new Semester();
        semester.setActive(dto.getActive() == null || dto.getActive());
        applyDto(semester, dto);
        Semester saved = semesterRepository.save(semester);
        markAllReportsStaleSafely();
        return convertToDTO(saved);
    }

    @Transactional
    public AcademicCalendarDTO createSemesterNameOnly(String semesterName, String academicYear) {
        if (semesterName == null || semesterName.isBlank()) {
            throw new IllegalArgumentException("Semester name is required");
        }
        if (academicYear == null || academicYear.isBlank()) {
            throw new IllegalArgumentException("Academic year is required");
        }

        LocalDate today = LocalDate.now();
        Semester semester = new Semester();
        semester.setSemesterName(semesterName.trim());
        semester.setAcademicYear(academicYear.trim());
        semester.setSemesterStartDate(today);
        semester.setSemesterEndDate(today.plusMonths(5));
        semester.setActive(true);

        Semester saved = semesterRepository.save(semester);
        markAllReportsStaleSafely();
        return convertToDTO(saved);
    }

    @Transactional
    public AcademicCalendarDTO renameSemester(Long semesterId, String semesterName, String academicYear) {
        if (semesterName == null || semesterName.isBlank()) {
            throw new IllegalArgumentException("Semester name is required");
        }
        if (academicYear == null || academicYear.isBlank()) {
            throw new IllegalArgumentException("Academic year is required");
        }

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));

        semester.setSemesterName(semesterName.trim());
        semester.setAcademicYear(academicYear.trim());
        Semester saved = semesterRepository.save(semester);
        markAllReportsStaleSafely();
        return convertToDTO(saved);
    }

    @Transactional
    public AcademicCalendarDTO updateSemester(Long semesterId, AcademicCalendarDTO dto) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
        applyDto(semester, dto);
        Semester saved = semesterRepository.save(semester);

        List<Long> targetSemesterIds = dto.getTargetSemesterIds();
        if (targetSemesterIds != null && !targetSemesterIds.isEmpty()) {
            propagateExamDatesToSelectedSemesters(saved, targetSemesterIds);
        }

        studentServiceProvider.ifAvailable(StudentService::markAllAttendanceReportsAsStale);
        return convertToDTO(saved);
    }

    private void propagateExamDatesToSelectedSemesters(Semester source, List<Long> targetSemesterIds) {
        List<Long> validTargetIds = new ArrayList<>();
        for (Long id : targetSemesterIds) {
            if (id != null && !id.equals(source.getId()) && !validTargetIds.contains(id)) {
                validTargetIds.add(id);
            }
        }

        if (validTargetIds.isEmpty()) {
            return;
        }

        List<Semester> targets = semesterRepository.findAllById(validTargetIds);
        for (Semester target : targets) {
            target.setCat1StartDate(source.getCat1StartDate());
            target.setCat1EndDate(source.getCat1EndDate());
            target.setCat2StartDate(source.getCat2StartDate());
            target.setCat2EndDate(source.getCat2EndDate());
            target.setFatStartDate(source.getFatStartDate());
            target.setFatEndDate(source.getFatEndDate());
        }
        if (!targets.isEmpty()) {
            semesterRepository.saveAll(targets);
        }
    }

    @Transactional
    public AcademicCalendarDTO setSemesterActive(Long semesterId, boolean active) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
        semester.setActive(active);
        Semester saved = semesterRepository.save(semester);
        markAllReportsStaleSafely();
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteSemester(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));

        long linkedCourses = courseRepository.countBySemesterId(semesterId);
        long linkedHolidays = holidayRepository.countBySemesterId(semesterId);
        long linkedRequests = studentHolidayRequestRepository.countBySemesterId(semesterId);
        long linkedStudents = userRepository.countByCurrentSemester_Id(semesterId);

        if (linkedCourses > 0 || linkedHolidays > 0 || linkedRequests > 0 || linkedStudents > 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Cannot delete semester '%s'. It is in use by %d course(s), %d holiday record(s), %d holiday request(s), and %d student profile(s).",
                            semester.getSemesterName(),
                            linkedCourses,
                            linkedHolidays,
                            linkedRequests,
                            linkedStudents
                    )
            );
        }

        semesterRepository.delete(semester);
        markAllReportsStaleSafely();
    }

    private void markAllReportsStaleSafely() {
        studentServiceProvider.ifAvailable(studentService -> {
            try {
                studentService.markAllAttendanceReportsAsStale();
            } catch (Exception e) {
                log.error("Error marking reports stale after semester change", e);
            }
        });
    }

    public List<AcademicCalendarDTO> getAllSemesters() {
        return semesterRepository.findAllByOrderBySemesterStartDateDesc()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<AcademicCalendarDTO> getActiveSemesters() {
        return semesterRepository.findByActiveTrueOrderBySemesterStartDateDesc()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public Optional<AcademicCalendarDTO> getCurrentAcademicCalendar() {
        if (semesterRepository == null) {
            List<AcademicCalendar> calendars = academicCalendarRepository.findAll();
            if (calendars.isEmpty()) {
                return Optional.empty();
            }

            AcademicCalendar c = calendars.get(0);
            return Optional.ofNullable(c).map(cal -> {
                AcademicCalendarDTO dto = new AcademicCalendarDTO();
                dto.setId(cal.getId());
                dto.setAcademicYear(cal.getAcademicYear());
                dto.setSemesterStartDate(cal.getSemesterStartDate());
                dto.setExamStartDate(cal.getExamStartDate());
                dto.setCat1StartDate(cal.getCat1StartDate());
                dto.setCat1EndDate(cal.getCat1EndDate());
                dto.setCat2StartDate(cal.getCat2StartDate());
                dto.setCat2EndDate(cal.getCat2EndDate());
                dto.setFatStartDate(cal.getFatStartDate());
                dto.setFatEndDate(cal.getFatEndDate());
                return dto;
            });
        }

        return semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc()
                .map(this::convertToDTO);
    }

    public Optional<Semester> getCurrentSemesterEntity() {
        return semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc();
    }

    public Optional<Semester> getSemesterById(Long semesterId) {
        return semesterRepository.findById(semesterId);
    }

    private void applyDto(Semester semester, AcademicCalendarDTO dto) {
        if (dto.getSemesterName() != null && !dto.getSemesterName().isBlank()) {
            semester.setSemesterName(dto.getSemesterName().trim());
        } else if (semester.getSemesterName() == null || semester.getSemesterName().isBlank()) {
            semester.setSemesterName("Semester");
        }

        semester.setAcademicYear(dto.getAcademicYear());
        semester.setSemesterStartDate(dto.getSemesterStartDate());
        semester.setSemesterEndDate(dto.getExamStartDate());
        semester.setCat1StartDate(dto.getCat1StartDate());
        semester.setCat1EndDate(dto.getCat1EndDate());
        semester.setCat2StartDate(dto.getCat2StartDate());
        semester.setCat2EndDate(dto.getCat2EndDate());
        semester.setFatStartDate(dto.getFatStartDate());
        semester.setFatEndDate(dto.getFatEndDate());

        if (dto.getActive() != null) {
            semester.setActive(dto.getActive());
        }
    }

    private AcademicCalendarDTO convertToDTO(Semester semester) {
        AcademicCalendarDTO dto = new AcademicCalendarDTO();
        dto.setId(semester.getId());
        dto.setSemesterName(semester.getSemesterName());
        dto.setAcademicYear(semester.getAcademicYear());
        dto.setSemesterStartDate(semester.getSemesterStartDate());
        dto.setExamStartDate(semester.getSemesterEndDate());
        dto.setCat1StartDate(semester.getCat1StartDate());
        dto.setCat1EndDate(semester.getCat1EndDate());
        dto.setCat2StartDate(semester.getCat2StartDate());
        dto.setCat2EndDate(semester.getCat2EndDate());
        dto.setFatStartDate(semester.getFatStartDate());
        dto.setFatEndDate(semester.getFatEndDate());
        dto.setActive(semester.getActive());
        dto.setCreatedAt(semester.getCreatedAt());
        dto.setUpdatedAt(semester.getUpdatedAt());
        return dto;
    }
}
