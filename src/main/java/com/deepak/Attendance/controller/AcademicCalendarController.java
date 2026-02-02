package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.AcademicCalendarDTO;
import com.deepak.Attendance.service.AcademicCalendarService;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/academic-calendar")
public class AcademicCalendarController {

    @Autowired
    private AcademicCalendarService academicCalendarService;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private UserRepository userRepository;

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setAcademicCalendar(@RequestBody AcademicCalendarDTO dto) {
        try {
            AcademicCalendarDTO saved = academicCalendarService.saveOrUpdateAcademicCalendar(dto);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAcademicCalendar() {
        var calendar = academicCalendarService.getCurrentAcademicCalendar();
        if (calendar.isPresent()) {
            return ResponseEntity.ok(calendar.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCurrentAcademicCalendar() {
        var calendar = academicCalendarService.getCurrentAcademicCalendar();
        if (calendar.isPresent()) {
            return ResponseEntity.ok(calendar.get());
        }
        // Return empty calendar if not found
        AcademicCalendarDTO emptyCalendar = new AcademicCalendarDTO();
        return ResponseEntity.ok(emptyCalendar);
    }
    
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", userRepository.count() - 1); // Exclude admin user
        stats.put("totalCourses", courseRepository.count());
        return ResponseEntity.ok(stats);
    }
}

// Student endpoint for academic calendar - separate controller
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/student/academic-calendar")
class StudentAcademicCalendarController {

    @Autowired
    private AcademicCalendarService academicCalendarService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getAcademicCalendar() {
        var calendar = academicCalendarService.getCurrentAcademicCalendar();
        if (calendar.isPresent()) {
            return ResponseEntity.ok(calendar.get());
        }
        // Return empty calendar if not found
        AcademicCalendarDTO emptyCalendar = new AcademicCalendarDTO();
        return ResponseEntity.ok(emptyCalendar);
    }
}
