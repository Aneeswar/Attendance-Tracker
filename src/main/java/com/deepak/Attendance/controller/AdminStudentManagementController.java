package com.deepak.Attendance.controller;

import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.entity.Course;
import com.deepak.Attendance.entity.AttendanceReport;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.AttendanceReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/students")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminStudentManagementController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceReportRepository attendanceReportRepository;

    /**
     * Get all students with pagination
     * GET /api/admin/students?page=0&size=10&sort=id&sortDir=desc
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<User> students = userRepository.findByRole("STUDENT", pageable);
            
            List<Map<String, Object>> studentList = students.getContent().stream()
                    .map(student -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", student.getId());
                        map.put("username", student.getUsername());
                        map.put("email", student.getEmail());
                        map.put("coursesCount", courseRepository.countByUserId(student.getId()));
                        return map;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("students", studentList);
            response.put("totalElements", students.getTotalElements());
            response.put("totalPages", students.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch students: " + e.getMessage());
                    }});
        }
    }

    /**
     * Get student detail with courses and attendance
     * GET /api/admin/students/{studentId}
     */
    @GetMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStudentDetail(@PathVariable Long studentId) {
        try {
            Optional<User> student = userRepository.findById(studentId);
            
            if (student.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "Student not found");
                        }});
            }
            
            User userData = student.get();
            List<Course> courses = courseRepository.findByUserId(studentId);
            List<AttendanceReport> attendanceReports = attendanceReportRepository.findByCourse_UserId(studentId);
            
            // Calculate aggregate attendance statistics
            double totalAttendancePercentage = 0;
            int reportCount = 0;
            
            if (!attendanceReports.isEmpty()) {
                for (AttendanceReport report : attendanceReports) {
                    if (report.getCurrentPercentage() != null) {
                        totalAttendancePercentage += report.getCurrentPercentage();
                        reportCount++;
                    }
                }
            }
            
            double averageAttendance = reportCount > 0 ? totalAttendancePercentage / reportCount : 0;
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", userData.getId());
            response.put("username", userData.getUsername());
            response.put("email", userData.getEmail());
            response.put("coursesCount", courses.size());
            response.put("courses", courses.stream().map(c -> {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", c.getId());
                courseMap.put("code", c.getCourseCode());
                courseMap.put("name", c.getCourseName());
                courseMap.put("startDate", c.getCourseStartDate());
                return courseMap;
            }).collect(Collectors.toList()));
            response.put("averageAttendance", String.format("%.2f", averageAttendance));
            response.put("attendanceCount", attendanceReports.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching student details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch student details: " + e.getMessage());
                    }});
        }
    }

    /**
     * Search students by username or email
     * GET /api/admin/students/search?q=searchTerm
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchStudents(@RequestParam String q) {
        try {
            List<User> students = userRepository.findByRoleAndSearchTerm("STUDENT", q);
            
            List<Map<String, Object>> studentList = students.stream()
                    .map(student -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", student.getId());
                        map.put("username", student.getUsername());
                        map.put("email", student.getEmail());
                        map.put("coursesCount", courseRepository.countByUserId(student.getId()));
                        return map;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("results", studentList);
            response.put("count", studentList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to search students: " + e.getMessage());
                    }});
        }
    }

    /**
     * Get attendance report for a specific student
     * GET /api/admin/students/{studentId}/attendance-report
     */
    @GetMapping("/{studentId}/attendance-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStudentAttendanceReport(@PathVariable Long studentId) {
        try {
            Optional<User> student = userRepository.findById(studentId);
            
            if (student.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "Student not found");
                        }});
            }
            
            List<AttendanceReport> reports = attendanceReportRepository.findByCourse_UserId(studentId);
            
            List<Map<String, Object>> reportList = reports.stream()
                    .map(report -> {
                        Map<String, Object> map = new HashMap<>();
                        if (report.getCourse() != null) {
                            map.put("courseId", report.getCourse().getId());
                            map.put("courseName", report.getCourse().getCourseName());
                            map.put("courseCode", report.getCourse().getCourseCode());
                        }
                        map.put("attendance", String.format("%.2f", report.getCurrentPercentage()));
                        map.put("classesAttended", report.getClassesAttended());
                        map.put("totalClasses", report.getTotalClassesConducted());
                        map.put("status", report.getStatus());
                        map.put("upcomingExamEligible75", report.getUpcomingExamEligible75());
                        map.put("upcomingExamEligible65", report.getUpcomingExamEligible65());
                        map.put("lastCalculated", report.getCalculatedAt());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("studentId", studentId);
            response.put("studentName", student.get().getUsername());
            response.put("reports", reportList);
            response.put("totalCourses", reportList.size());
            response.put("generatedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching student attendance report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch attendance report: " + e.getMessage());
                    }});
        }
    }

    /**
     * Get overall attendance statistics
     * GET /api/admin/students/stats/overall
     */
    @GetMapping("/stats/overall")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOverallAttendanceStats() {
        try {
            List<User> allStudents = userRepository.findByRole("STUDENT");
            List<AttendanceReport> allReports = attendanceReportRepository.findAll();
            
            // Calculate statistics
            double totalPercentage = 0;
            int reportCount = allReports.size();
            final int[] excellentAttendance = {0};
            final int[] goodAttendance = {0};
            final int[] moderateAttendance = {0};
            final int[] poorAttendance = {0};
            
            for (AttendanceReport report : allReports) {
                if (report.getCurrentPercentage() != null) {
                    double percentage = report.getCurrentPercentage();
                    totalPercentage += percentage;
                    
                    if (percentage > 85) excellentAttendance[0]++;
                    else if (percentage >= 75) goodAttendance[0]++;
                    else if (percentage >= 65) moderateAttendance[0]++;
                    else poorAttendance[0]++;
                }
            }
            
            double averageAttendance = reportCount > 0 ? totalPercentage / reportCount : 0;
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalStudents", allStudents.size());
            response.put("totalCourses", courseRepository.count());
            response.put("totalAttendanceRecords", reportCount);
            response.put("averageAttendance", String.format("%.2f", averageAttendance) + "%");
            response.put("distribution", new HashMap<String, Object>() {{
                put("excellent", excellentAttendance[0]);
                put("good", goodAttendance[0]);
                put("moderate", moderateAttendance[0]);
                put("poor", poorAttendance[0]);
            }});
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching overall attendance stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch statistics: " + e.getMessage());
                    }});
        }
    }

    /**
     * Get students with low attendance (below threshold)
     * GET /api/admin/students/low-attendance?threshold=65
     */
    @GetMapping("/low-attendance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStudentsWithLowAttendance(
            @RequestParam(defaultValue = "65") double threshold) {
        try {
            List<AttendanceReport> reports = attendanceReportRepository.findAll();
            
            Map<Long, List<Map<String, Object>>> studentLowAttendance = new HashMap<>();
            
            for (AttendanceReport report : reports) {
                if (report.getCurrentPercentage() != null && 
                    report.getCurrentPercentage() < threshold &&
                    report.getCourse() != null) {
                    
                    Long studentId = report.getCourse().getUserId();
                    studentLowAttendance.putIfAbsent(studentId, new ArrayList<>());
                    
                    Map<String, Object> courseInfo = new HashMap<>();
                    courseInfo.put("courseCode", report.getCourse().getCourseCode());
                    courseInfo.put("courseName", report.getCourse().getCourseName());
                    courseInfo.put("attendance", String.format("%.2f", report.getCurrentPercentage()));
                    courseInfo.put("classesAttended", report.getClassesAttended());
                    courseInfo.put("totalClasses", report.getTotalClassesConducted());
                    
                    studentLowAttendance.get(studentId).add(courseInfo);
                }
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<Long, List<Map<String, Object>>> entry : studentLowAttendance.entrySet()) {
                Optional<User> student = userRepository.findById(entry.getKey());
                if (student.isPresent()) {
                    Map<String, Object> studentInfo = new HashMap<>();
                    studentInfo.put("studentId", student.get().getId());
                    studentInfo.put("username", student.get().getUsername());
                    studentInfo.put("email", student.get().getEmail());
                    studentInfo.put("coursesWithLowAttendance", entry.getValue());
                    result.add(studentInfo);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("threshold", threshold + "%");
            response.put("studentsAffected", result.size());
            response.put("students", result);
            response.put("generatedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching low attendance students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch low attendance data: " + e.getMessage());
                    }});
        }
    }

    /**
     * Export attendance report as summary
     * GET /api/admin/students/report/summary
     */
    @GetMapping("/report/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAttendanceSummaryReport() {
        try {
            List<User> students = userRepository.findByRole("STUDENT");
            List<Map<String, Object>> summaryList = new ArrayList<>();
            
            for (User student : students) {
                List<AttendanceReport> reports = attendanceReportRepository.findByCourse_UserId(student.getId());
                
                if (!reports.isEmpty()) {
                    double totalAttendance = 0;
                    int eligibleCount = 0;
                    for (AttendanceReport report : reports) {
                        if (report.getCurrentPercentage() != null) {
                            totalAttendance += report.getCurrentPercentage();
                        }
                        if (report.getUpcomingExamEligible75() != null && report.getUpcomingExamEligible75()) {
                            eligibleCount++;
                        }
                    }
                    double averageAttendance = totalAttendance / reports.size();
                    
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("studentId", student.getId());
                    summary.put("username", student.getUsername());
                    summary.put("email", student.getEmail());
                    summary.put("enrolledCourses", reports.size());
                    summary.put("averageAttendance", String.format("%.2f", averageAttendance));
                    summary.put("examEligible", eligibleCount == reports.size());
                    
                    summaryList.add(summary);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("summaryReport", summaryList);
            response.put("totalStudentsReported", summaryList.size());
            response.put("generatedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating summary report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to generate report: " + e.getMessage());
                    }});
        }
    }
}
