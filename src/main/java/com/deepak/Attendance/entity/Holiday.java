package com.deepak.Attendance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "holidays", uniqueConstraints = @UniqueConstraint(columnNames = {"semester_id", "date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long academicCalendarId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HolidayType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HolidayScope scope = HolidayScope.FULL; // FULL, MORNING, AFTERNOON

    @Column(nullable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }

    public enum HolidayType {
        PUBLIC, ACADEMIC, RESTRICTED, CALENDAR, EXTRA
    }

    public enum HolidayScope {
        FULL, MORNING, AFTERNOON
    }
}
