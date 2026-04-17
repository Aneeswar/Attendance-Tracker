package com.deepak.Attendance.config;

import com.deepak.Attendance.entity.Course;
import com.deepak.Attendance.entity.Holiday;
import com.deepak.Attendance.entity.Semester;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import com.deepak.Attendance.repository.SemesterRepository;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class SemesterOneBackfillRunner implements CommandLineRunner {

    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final HolidayRepository holidayRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<StudentService> studentServiceProvider;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.migration.map-sem1-on-startup:false}")
    private boolean mapSem1OnStartup;

    public SemesterOneBackfillRunner(SemesterRepository semesterRepository,
                                     CourseRepository courseRepository,
                                     HolidayRepository holidayRepository,
                                     UserRepository userRepository,
                                     ObjectProvider<StudentService> studentServiceProvider,
                                     JdbcTemplate jdbcTemplate) {
        this.semesterRepository = semesterRepository;
        this.courseRepository = courseRepository;
        this.holidayRepository = holidayRepository;
        this.userRepository = userRepository;
        this.studentServiceProvider = studentServiceProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!mapSem1OnStartup) {
            log.info("Sem1 backfill disabled (app.migration.map-sem1-on-startup=false)");
            return;
        }

        ensureSemesterTableColumns();

        Semester sem1 = ensureSem1();

        int courseUpdates = mapLegacyCoursesToSem1(sem1);
        int holidayUpdates = mapLegacyHolidaysToSem1(sem1);
        int userUpdates = mapStudentsWithoutSemesterToSem1(sem1);

        if (courseUpdates > 0 || holidayUpdates > 0 || userUpdates > 0) {
            log.info("Sem1 backfill applied: courses={}, holidays={}, users={}", courseUpdates, holidayUpdates, userUpdates);
            studentServiceProvider.ifAvailable(StudentService::markAllAttendanceReportsAsStale);
        } else {
            log.info("Sem1 backfill skipped: no legacy rows found");
        }
    }

    private Semester ensureSem1() {
        return semesterRepository.findFirstBySemesterNameIgnoreCaseOrderByIdAsc("Semester 1")
                .orElseGet(() -> {
                    LocalDate now = LocalDate.now();
                    Semester sem1 = new Semester();
                    sem1.setSemesterName("Semester 1");
                    sem1.setAcademicYear(now.getYear() + "-" + (now.getYear() + 1));
                    sem1.setSemesterStartDate(LocalDate.of(now.getYear(), 1, 1));
                    sem1.setSemesterEndDate(LocalDate.of(now.getYear(), 12, 31));
                    sem1.setActive(true);
                    return semesterRepository.save(sem1);
                });
    }

    private void ensureSemesterTableColumns() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS semesters (
                    id BIGSERIAL PRIMARY KEY,
                    semester_name VARCHAR(255) NOT NULL,
                    academic_year VARCHAR(255),
                    semester_start_date DATE,
                    semester_end_date DATE,
                    cat1_start_date DATE,
                    cat1_end_date DATE,
                    cat2_start_date DATE,
                    cat2_end_date DATE,
                    fat_start_date DATE,
                    fat_end_date DATE,
                    active BOOLEAN,
                    created_at DATE,
                    updated_at DATE
                )
                """);

        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS semester_name VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS academic_year VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS semester_start_date DATE");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS semester_end_date DATE");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS cat1_start_date DATE");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS cat1_end_date DATE");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS cat2_start_date DATE");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS cat2_end_date DATE");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS fat_start_date DATE");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS fat_end_date DATE");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS active BOOLEAN");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS created_at DATE");
        jdbcTemplate.execute("ALTER TABLE semesters ADD COLUMN IF NOT EXISTS updated_at DATE");

        // Legacy schemas may still have old required columns that are no longer mapped.
        dropNotNullIfColumnExists("semesters", "name");
        dropNotNullIfColumnExists("semesters", "start_date");
        dropNotNullIfColumnExists("semesters", "end_date");
        }

        private void dropNotNullIfColumnExists(String tableName, String columnName) {
        Integer columnCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                  AND column_name = ?
                """,
            Integer.class,
            tableName,
            columnName
        );

        if (columnCount != null && columnCount > 0) {
            jdbcTemplate.execute(
                "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " DROP NOT NULL"
            );
        }
    }

    private int mapLegacyCoursesToSem1(Semester sem1) {
        List<Course> courses = courseRepository.findBySemesterIsNull();
        for (Course course : courses) {
            course.setSemester(sem1);
        }
        if (!courses.isEmpty()) {
            courseRepository.saveAll(courses);
        }
        return courses.size();
    }

    private int mapLegacyHolidaysToSem1(Semester sem1) {
        List<Holiday> holidays = holidayRepository.findBySemesterIsNull();
        for (Holiday holiday : holidays) {
            holiday.setSemester(sem1);
            if (holiday.getAcademicCalendarId() == null) {
                holiday.setAcademicCalendarId(sem1.getId());
            }
        }
        if (!holidays.isEmpty()) {
            holidayRepository.saveAll(holidays);
        }
        return holidays.size();
    }

    private int mapStudentsWithoutSemesterToSem1(Semester sem1) {
        List<User> users = userRepository.findByCurrentSemesterIsNull();
        for (User user : users) {
            user.setCurrentSemester(sem1);
        }
        if (!users.isEmpty()) {
            userRepository.saveAll(users);
        }
        return users.size();
    }
}