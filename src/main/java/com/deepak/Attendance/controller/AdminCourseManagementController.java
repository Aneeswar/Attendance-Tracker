package com.deepak.Attendance.controller;

import com.deepak.Attendance.entity.Course;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/courses")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminCourseManagementController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentService studentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String q) {
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Course> coursesPage;
            if (q != null && !q.trim().isEmpty()) {
                String search = q.trim();
                coursesPage = courseRepository.findByCourseCatalog_CourseCodeContainingIgnoreCaseOrCourseCatalog_CourseNameContainingIgnoreCase(search, search, pageable);
            } else {
                coursesPage = courseRepository.findAll(pageable);
            }

            List<Map<String, Object>> rows = coursesPage.getContent().stream().map(c -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", c.getId());
                row.put("courseCode", c.getCourseCode());
                row.put("courseName", c.getCourseName());
                row.put("slot", c.getSlot());
                row.put("courseStartDate", c.getCourseStartDate());
                row.put("studentId", c.getUserId());
                row.put("studentUsername", userRepository.findById(c.getUserId()).map(u -> u.getUsername()).orElse("Unknown"));
                return row;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("courses", rows);
            response.put("totalElements", coursesPage.getTotalElements());
            response.put("totalPages", coursesPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch courses: " + e.getMessage());
                    }});
        }
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId) {
        try {
            Optional<Course> course = courseRepository.findById(courseId);
            if (course.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "Course not found");
                        }});
            }

            Course c = course.get();
            studentService.deleteCourse(c.getUserId(), c.getId());
            log.info("Admin deleted course {} ({})", c.getCourseCode(), c.getId());

            return ResponseEntity.ok(new HashMap<String, String>() {{
                put("message", "Course deleted successfully");
            }});
        } catch (Exception e) {
            log.error("Error deleting course {}", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to delete course: " + e.getMessage());
                    }});
        }
    }
}
