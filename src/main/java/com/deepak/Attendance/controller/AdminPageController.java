package com.deepak.Attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "admin-dashboard";
    }

    @GetMapping("/profile")
    public String profilePage() {
        return "admin-profile";
    }

    @GetMapping("/holidays")
    public String adminHolidays() {
        return "admin-holidays";
    }

    @GetMapping("/academic-calendar")
    public String adminAcademicCalendar() {
        return "admin-academic-calendar";
    }

    @GetMapping("/semesters")
    public String adminSemesters() {
        return "admin-semesters";
    }

    @GetMapping("/students")
    public String adminStudents() {
        return "admin-students";
    }

    @GetMapping("/holiday-requests")
    public String adminHolidayRequests() {
        return "admin-holiday-requests";
    }

    @GetMapping("/courses")
    public String adminCourses() {
        return "admin-courses";
    }
}
