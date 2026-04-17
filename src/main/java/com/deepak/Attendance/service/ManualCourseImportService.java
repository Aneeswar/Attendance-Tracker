package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.ManualCourseImportConfirmResponse;
import com.deepak.Attendance.dto.ManualCourseImportPreviewRequest;
import com.deepak.Attendance.dto.ManualCourseImportPreviewResponse;
import com.deepak.Attendance.dto.ManualCourseImportPreviewRowDTO;
import com.deepak.Attendance.dto.ManualCourseImportRowRequest;
import com.deepak.Attendance.entity.Course;
import com.deepak.Attendance.entity.CourseCatalog;
import com.deepak.Attendance.entity.Semester;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.entity.enums.CourseType;
import com.deepak.Attendance.entity.enums.SessionType;
import com.deepak.Attendance.repository.CourseCatalogRepository;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ManualCourseImportService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseCatalogRepository courseCatalogRepository;
    private final SlotTokenParserService slotTokenParserService;
    private final MasterTimetableSlotResolverService slotResolverService;
    private final CourseScheduleGenerationService courseScheduleGenerationService;
    private final StudentService studentService;

    public ManualCourseImportService(UserRepository userRepository,
                                     CourseRepository courseRepository,
                                     CourseCatalogRepository courseCatalogRepository,
                                     SlotTokenParserService slotTokenParserService,
                                     MasterTimetableSlotResolverService slotResolverService,
                                     CourseScheduleGenerationService courseScheduleGenerationService,
                                     StudentService studentService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.courseCatalogRepository = courseCatalogRepository;
        this.slotTokenParserService = slotTokenParserService;
        this.slotResolverService = slotResolverService;
        this.courseScheduleGenerationService = courseScheduleGenerationService;
        this.studentService = studentService;
    }

    public ManualCourseImportPreviewResponse preview(Long userId, ManualCourseImportPreviewRequest request) {
        Semester semester = resolveSelectedSemester(userId);

        List<ManualCourseImportPreviewRowDTO> rows = new ArrayList<>();
        List<ManualCourseImportRowRequest> requestRows = request == null ? List.of() : request.getRows();
        if (requestRows == null || requestRows.isEmpty()) {
            throw new IllegalArgumentException("At least one course row is required");
        }

        for (int i = 0; i < requestRows.size(); i++) {
            rows.add(validateAndResolveRow(i + 1, requestRows.get(i), semester));
        }

        boolean valid = rows.stream().allMatch(ManualCourseImportPreviewRowDTO::isValid);
        return new ManualCourseImportPreviewResponse(valid, semester.getSemesterName(), semester.getId(), rows);
    }

    @Transactional
    public ManualCourseImportConfirmResponse confirm(Long userId, ManualCourseImportPreviewRequest request) {
        ManualCourseImportPreviewResponse preview = preview(userId, request);
        if (!preview.isValid()) {
            throw new IllegalArgumentException("Cannot confirm import. Fix validation errors in preview response.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Semester semester = resolveSelectedSemester(userId);

        int created = 0;
        int updated = 0;
        List<String> createdCodes = new ArrayList<>();
        List<String> updatedCodes = new ArrayList<>();

        for (ManualCourseImportPreviewRowDTO row : preview.getRows()) {
            String code = normalizeCode(row.getCourseCode());
            String name = normalizeName(row.getCourseName());
            CourseType courseType = CourseType.valueOf(row.getCourseType().toUpperCase(Locale.ROOT));
            CourseCatalog catalog = resolveOrCreateCourseCatalog(code, name);

            Optional<Course> existing = courseRepository.findByUserIdAndSemesterIdAndCourseCatalog_CourseCodeIgnoreCase(userId, semester.getId(), code)
                    .or(() -> courseRepository.findByUserIdAndSemesterIdAndCourseCodeIgnoreCase(userId, semester.getId(), code));

            Course course;
            boolean isCreate = existing.isEmpty();
            if (isCreate) {
                course = new Course();
                course.setUserId(user.getId());
                course.setSemester(semester);
            } else {
                course = existing.get();
            }

            course.setCourseCatalog(catalog);
            course.setCourseCode(code);
            course.setCourseName(name);
            course.setSlot(String.join("+", row.getNormalizedTokens()));
            course.setRegisteredDate(row.getRegisteredDate());
            course.setCourseStartDate(row.getEffectiveStartDate());
            course.setCourseType(courseType);
            course = courseRepository.save(course);

            courseScheduleGenerationService.replaceGeneratedSchedules(course, row.getResolvedSchedule());

            if (isCreate) {
                initializeNewCourseAttendanceAsPresent(userId, course);
            }

            if (isCreate) {
                created++;
                createdCodes.add(code);
            } else {
                updated++;
                updatedCodes.add(code);
            }
        }

        studentService.evictAttendanceCache(userId);

        ManualCourseImportConfirmResponse response = new ManualCourseImportConfirmResponse();
        response.setMessage("Manual course import completed successfully");
        response.setSemesterId(semester.getId());
        response.setSemesterName(semester.getSemesterName());
        response.setProcessedRows(preview.getRows().size());
        response.setCreatedCount(created);
        response.setUpdatedCount(updated);
        response.setCreatedCourseCodes(createdCodes);
        response.setUpdatedCourseCodes(updatedCodes);
        return response;
    }

    private ManualCourseImportPreviewRowDTO validateAndResolveRow(int rowNumber, ManualCourseImportRowRequest row, Semester semester) {
        ManualCourseImportPreviewRowDTO previewRow = new ManualCourseImportPreviewRowDTO();
        previewRow.setRowNumber(rowNumber);

        String code = row == null ? null : row.getCourseCode();
        String name = row == null ? null : row.getCourseName();
        String slots = row == null ? null : row.getSlotString();
        String type = row == null ? null : row.getCourseType();

        previewRow.setCourseCode(code);
        previewRow.setCourseName(name);
        previewRow.setSlotString(slots);
        previewRow.setCourseType(type);
        previewRow.setRegisteredDate(row == null ? null : row.getRegisteredDate());
        previewRow.setFieldConfidence(row != null && row.getFieldConfidence() != null ? row.getFieldConfidence() : new java.util.HashMap<>());
        previewRow.setSuggestions(row != null && row.getSuggestions() != null ? row.getSuggestions() : new java.util.HashMap<>());

        if (isBlank(code)) {
            previewRow.getErrors().add("courseCode is required");
        }
        if (isBlank(name)) {
            previewRow.getErrors().add("courseName is required");
        }
        if (isBlank(slots)) {
            previewRow.getErrors().add("slotString is required");
        }
        if (previewRow.getRegisteredDate() == null) {
            previewRow.getErrors().add("registeredDate is required");
        }

        if (previewRow.getRegisteredDate() != null) {
            previewRow.setEffectiveStartDate(computeEffectiveStartDate(semester, previewRow.getRegisteredDate()));
        }

        CourseType courseType = null;
        if (isBlank(type)) {
            previewRow.getErrors().add("courseType is required and must be THEORY or LAB");
        } else {
            try {
                courseType = CourseType.valueOf(type.trim().toUpperCase(Locale.ROOT));
                previewRow.setCourseType(courseType.name());
            } catch (IllegalArgumentException ex) {
                previewRow.getErrors().add("courseType must be THEORY or LAB");
            }
        }

        List<String> tokens = slotTokenParserService.parseUniqueTokens(slots);
        previewRow.setNormalizedTokens(tokens);
        if (tokens.isEmpty()) {
            previewRow.getErrors().add("slotString must contain at least one token");
        }

        if (!previewRow.getErrors().isEmpty()) {
            return previewRow;
        }

        SessionType expectedType = courseType == CourseType.LAB ? SessionType.LAB : SessionType.THEORY;
        MasterTimetableSlotResolverService.ResolutionResult resolved = slotResolverService.resolveTokens(tokens, expectedType);
        previewRow.getErrors().addAll(resolved.getErrors());
        previewRow.setResolvedSchedule(resolved.getRows());
        return previewRow;
    }

    private Semester resolveSelectedSemester(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getCurrentSemester() == null || user.getCurrentSemester().getId() == null) {
            throw new IllegalArgumentException("No semester selected in profile. Please select a semester before importing courses.");
        }

        return user.getCurrentSemester();
    }

    private CourseCatalog resolveOrCreateCourseCatalog(String code, String name) {
        return courseCatalogRepository.findByCourseCodeAndCourseName(code, name)
                .orElseGet(() -> courseCatalogRepository.save(new CourseCatalog(null, code, name)));
    }

    private LocalDate computeEffectiveStartDate(Semester semester, LocalDate registeredDate) {
        LocalDate semesterStart = semester != null ? semester.getSemesterStartDate() : null;
        if (semesterStart == null) {
            return registeredDate;
        }
        return registeredDate.isAfter(semesterStart) ? registeredDate : semesterStart;
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void initializeNewCourseAttendanceAsPresent(Long userId, Course course) {
        try {
            int conductedClasses = studentService.getClassDatesForCourse(userId, course.getId()).size();
            studentService.saveAttendanceInput(userId, course.getCourseCode(), conductedClasses, conductedClasses);
        } catch (Exception ex) {
            // Non-fatal: course creation should still succeed even if default report bootstrap fails.
        }
    }
}
