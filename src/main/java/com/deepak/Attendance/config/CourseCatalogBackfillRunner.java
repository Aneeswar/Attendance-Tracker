package com.deepak.Attendance.config;

import com.deepak.Attendance.entity.Course;
import com.deepak.Attendance.entity.CourseCatalog;
import com.deepak.Attendance.repository.CourseCatalogRepository;
import com.deepak.Attendance.repository.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class CourseCatalogBackfillRunner implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final CourseCatalogRepository courseCatalogRepository;

    public CourseCatalogBackfillRunner(CourseRepository courseRepository, CourseCatalogRepository courseCatalogRepository) {
        this.courseRepository = courseRepository;
        this.courseCatalogRepository = courseCatalogRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<Course> courses = courseRepository.findAll();
        int updated = 0;

        for (Course course : courses) {
            if (course.getCourseCatalog() != null) {
                continue;
            }

            String code = course.getCourseCode() == null ? "" : course.getCourseCode().trim().toUpperCase();
            String name = course.getCourseName() == null ? "" : course.getCourseName().trim();

            CourseCatalog catalog = courseCatalogRepository.findByCourseCodeAndCourseName(code, name)
                    .orElseGet(() -> courseCatalogRepository.save(new CourseCatalog(null, code, name)));

            course.setCourseCatalog(catalog);
            course.setCourseCode(code);
            course.setCourseName(name);
            updated++;
        }

        if (updated > 0) {
            log.info("Backfilled course catalog links for {} courses", updated);
        }
    }
}
