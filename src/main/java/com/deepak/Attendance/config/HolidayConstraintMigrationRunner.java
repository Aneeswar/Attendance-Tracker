package com.deepak.Attendance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HolidayConstraintMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public HolidayConstraintMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // Drop legacy unique constraints that enforce global uniqueness on date.
        jdbcTemplate.execute("""
                DO $$
                DECLARE r RECORD;
                BEGIN
                  FOR r IN
                    SELECT tc.constraint_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                      ON tc.constraint_name = kcu.constraint_name
                     AND tc.table_schema = kcu.table_schema
                     AND tc.table_name = kcu.table_name
                    WHERE tc.table_schema = current_schema()
                      AND tc.table_name = 'holidays'
                      AND tc.constraint_type = 'UNIQUE'
                    GROUP BY tc.constraint_name
                    HAVING bool_or(kcu.column_name = 'date')
                       AND NOT bool_or(kcu.column_name = 'semester_id')
                  LOOP
                    EXECUTE format('ALTER TABLE holidays DROP CONSTRAINT IF EXISTS %I', r.constraint_name);
                  END LOOP;
                END $$;
                """);

        // Drop legacy standalone unique indexes on date when present.
        jdbcTemplate.execute("""
                DO $$
                DECLARE r RECORD;
                BEGIN
                  FOR r IN
                    SELECT indexname
                    FROM pg_indexes
                    WHERE schemaname = current_schema()
                      AND tablename = 'holidays'
                      AND indexdef ILIKE 'CREATE UNIQUE INDEX%'
                      AND indexdef ILIKE '%(date)%'
                      AND indexdef NOT ILIKE '%(semester_id, date)%'
                  LOOP
                    EXECUTE format('DROP INDEX IF EXISTS %I', r.indexname);
                  END LOOP;
                END $$;
                """);

        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_holidays_semester_date ON holidays (semester_id, date)");
        log.info("Holiday uniqueness migration ensured (semester_id, date)");
    }
}
