package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.HolidayDTO;
import com.deepak.Attendance.dto.BulkHolidayRequest;
import com.deepak.Attendance.dto.DateRangeHolidayRequest;
import com.deepak.Attendance.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/holidays")
public class HolidayController {

    @Autowired
    private HolidayService holidayService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addHoliday(@RequestBody HolidayDTO dto) {
        try {
            HolidayDTO saved = holidayService.addHoliday(dto);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> bulkAddHolidays(@RequestBody BulkHolidayRequest request) {
        try {
            List<HolidayDTO> saved = holidayService.bulkAddHolidays(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addHolidayRange(@RequestBody DateRangeHolidayRequest request) {
        try {
            List<HolidayDTO> saved = holidayService.addHolidayRange(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllHolidays() {
        List<HolidayDTO> holidays = holidayService.getAllHolidays();
        return ResponseEntity.ok(holidays);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteHoliday(@PathVariable Long id) {
        try {
            holidayService.deleteHoliday(id);
            return ResponseEntity.ok("Holiday deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
