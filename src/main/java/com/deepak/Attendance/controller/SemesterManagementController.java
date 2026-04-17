package com.deepak.Attendance.controller;

import com.deepak.Attendance.service.AcademicCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/semesters")
public class SemesterManagementController {

    private final AcademicCalendarService academicCalendarService;

    public SemesterManagementController(AcademicCalendarService academicCalendarService) {
        this.academicCalendarService = academicCalendarService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listSemesters(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? academicCalendarService.getActiveSemesters() : academicCalendarService.getAllSemesters());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSemester(@RequestBody Map<String, Object> payload) {
        try {
            String semesterName = payload.get("semesterName") != null ? String.valueOf(payload.get("semesterName")) : null;
            String academicYear = payload.get("academicYear") != null ? String.valueOf(payload.get("academicYear")) : null;
            return ResponseEntity.ok(academicCalendarService.createSemesterNameOnly(semesterName, academicYear));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{semesterId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSemester(@PathVariable Long semesterId, @RequestBody Map<String, Object> payload) {
        try {
            String semesterName = payload.get("semesterName") != null ? String.valueOf(payload.get("semesterName")) : null;
            String academicYear = payload.get("academicYear") != null ? String.valueOf(payload.get("academicYear")) : null;
            return ResponseEntity.ok(academicCalendarService.renameSemester(semesterId, semesterName, academicYear));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{semesterId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setSemesterActive(@PathVariable Long semesterId, @RequestParam boolean active) {
        try {
            return ResponseEntity.ok(academicCalendarService.setSemesterActive(semesterId, active));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{semesterId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSemester(@PathVariable Long semesterId) {
        try {
            academicCalendarService.deleteSemester(semesterId);
            return ResponseEntity.ok(Map.of("message", "Semester deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/student/semesters")
class StudentSemesterOptionsController {

    private final AcademicCalendarService academicCalendarService;

    StudentSemesterOptionsController(AcademicCalendarService academicCalendarService) {
        this.academicCalendarService = academicCalendarService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getActiveSemesters() {
        return ResponseEntity.ok(academicCalendarService.getActiveSemesters());
    }
}
