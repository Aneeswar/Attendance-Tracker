package com.deepak.Attendance.config;

import com.deepak.Attendance.entity.MasterTimetableSlot;
import com.deepak.Attendance.entity.enums.DayOfWeek;
import com.deepak.Attendance.entity.enums.SessionType;
import com.deepak.Attendance.repository.MasterTimetableSlotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class MasterTimetableSeeder implements CommandLineRunner {

    private final MasterTimetableSlotRepository masterTimetableSlotRepository;

    public MasterTimetableSeeder(MasterTimetableSlotRepository masterTimetableSlotRepository) {
        this.masterTimetableSlotRepository = masterTimetableSlotRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<MasterTimetableSlot> seeds = canonicalRows();
        List<MasterTimetableSlot> existing = masterTimetableSlotRepository.findAll();

        Map<String, MasterTimetableSlot> firstExistingByKey = new LinkedHashMap<>();
        List<MasterTimetableSlot> toDelete = new ArrayList<>();

        for (MasterTimetableSlot row : existing) {
            String key = key(row.getSlotName(), row.getDayOfWeek(), row.getSessionType(), row.getStartTime(), row.getEndTime());
            if (firstExistingByKey.putIfAbsent(key, row) != null) {
                toDelete.add(row);
            }
        }

        Set<String> canonicalKeys = new HashSet<>();
        for (MasterTimetableSlot seed : seeds) {
            canonicalKeys.add(key(seed.getSlotName(), seed.getDayOfWeek(), seed.getSessionType(), seed.getStartTime(), seed.getEndTime()));
        }

        for (MasterTimetableSlot row : firstExistingByKey.values()) {
            String rowKey = key(row.getSlotName(), row.getDayOfWeek(), row.getSessionType(), row.getStartTime(), row.getEndTime());
            if (!canonicalKeys.contains(rowKey)) {
                toDelete.add(row);
            }
        }

        List<MasterTimetableSlot> toInsert = new ArrayList<>();
        for (MasterTimetableSlot seed : seeds) {
            String seedKey = key(seed.getSlotName(), seed.getDayOfWeek(), seed.getSessionType(), seed.getStartTime(), seed.getEndTime());
            if (!firstExistingByKey.containsKey(seedKey)) {
                toInsert.add(seed);
            }
        }

        if (!toDelete.isEmpty()) {
            masterTimetableSlotRepository.deleteAll(toDelete);
        }

        if (!toInsert.isEmpty()) {
            masterTimetableSlotRepository.saveAll(toInsert);
        }

        log.info("Master timetable reconciliation done. Existing: {}, inserted: {}, removed: {}, canonical total: {}",
                existing.size(), toInsert.size(), toDelete.size(), seeds.size());
    }

    private List<MasterTimetableSlot> canonicalRows() {
        List<MasterTimetableSlot> seeds = new ArrayList<>();

        // THEORY - TUE
        seeds.add(row("TTF1", DayOfWeek.TUE, SessionType.THEORY, "08:00", "08:50"));
        seeds.add(row("A1", DayOfWeek.TUE, SessionType.THEORY, "09:00", "09:50"));
        seeds.add(row("B1", DayOfWeek.TUE, SessionType.THEORY, "10:00", "10:50"));
        seeds.add(row("TC1", DayOfWeek.TUE, SessionType.THEORY, "11:00", "11:50"));
        seeds.add(row("G1", DayOfWeek.TUE, SessionType.THEORY, "11:01", "11:51"));
        seeds.add(row("D1", DayOfWeek.TUE, SessionType.THEORY, "12:00", "12:50"));
        seeds.add(row("F2", DayOfWeek.TUE, SessionType.THEORY, "14:00", "14:50"));
        seeds.add(row("A2", DayOfWeek.TUE, SessionType.THEORY, "15:00", "15:50"));
        seeds.add(row("B2", DayOfWeek.TUE, SessionType.THEORY, "16:00", "16:50"));
        seeds.add(row("TC2", DayOfWeek.TUE, SessionType.THEORY, "17:00", "17:50"));
        seeds.add(row("G2", DayOfWeek.TUE, SessionType.THEORY, "17:01", "17:51"));
        seeds.add(row("TDD2", DayOfWeek.TUE, SessionType.THEORY, "18:00", "18:50"));

        // THEORY - WED
        seeds.add(row("TGG1", DayOfWeek.WED, SessionType.THEORY, "08:00", "08:50"));
        seeds.add(row("D1", DayOfWeek.WED, SessionType.THEORY, "09:00", "09:50"));
        seeds.add(row("F1", DayOfWeek.WED, SessionType.THEORY, "10:00", "10:50"));
        seeds.add(row("E1", DayOfWeek.WED, SessionType.THEORY, "11:00", "11:50"));
        seeds.add(row("SC2", DayOfWeek.WED, SessionType.THEORY, "11:01", "11:51"));
        seeds.add(row("B1", DayOfWeek.WED, SessionType.THEORY, "12:00", "12:50"));
        seeds.add(row("D2", DayOfWeek.WED, SessionType.THEORY, "14:00", "14:50"));
        seeds.add(row("TF2", DayOfWeek.WED, SessionType.THEORY, "15:00", "15:50"));
        seeds.add(row("G2", DayOfWeek.WED, SessionType.THEORY, "15:01", "15:51"));
        seeds.add(row("E2", DayOfWeek.WED, SessionType.THEORY, "16:00", "16:50"));
        seeds.add(row("SC1", DayOfWeek.WED, SessionType.THEORY, "16:01", "16:51"));
        seeds.add(row("B2", DayOfWeek.WED, SessionType.THEORY, "17:00", "17:50"));
        seeds.add(row("TCC2", DayOfWeek.WED, SessionType.THEORY, "18:00", "18:50"));

        // THEORY - THU
        seeds.add(row("TEE1", DayOfWeek.THU, SessionType.THEORY, "08:00", "08:50"));
        seeds.add(row("C1", DayOfWeek.THU, SessionType.THEORY, "09:00", "09:50"));
        seeds.add(row("TD1", DayOfWeek.THU, SessionType.THEORY, "10:00", "10:50"));
        seeds.add(row("TG1", DayOfWeek.THU, SessionType.THEORY, "10:01", "10:51"));
        seeds.add(row("TAA1", DayOfWeek.THU, SessionType.THEORY, "11:00", "11:50"));
        seeds.add(row("ECS", DayOfWeek.THU, SessionType.THEORY, "11:01", "11:51"));
        seeds.add(row("TBB1", DayOfWeek.THU, SessionType.THEORY, "12:00", "12:50"));
        seeds.add(row("CLUB", DayOfWeek.THU, SessionType.THEORY, "12:01", "12:51"));
        seeds.add(row("TE2", DayOfWeek.THU, SessionType.THEORY, "14:00", "14:50"));
        seeds.add(row("SE1", DayOfWeek.THU, SessionType.THEORY, "14:01", "14:51"));
        seeds.add(row("C2", DayOfWeek.THU, SessionType.THEORY, "15:00", "15:50"));
        seeds.add(row("A2", DayOfWeek.THU, SessionType.THEORY, "16:00", "16:50"));
        seeds.add(row("TD2", DayOfWeek.THU, SessionType.THEORY, "17:00", "17:50"));
        seeds.add(row("TG2", DayOfWeek.THU, SessionType.THEORY, "17:01", "17:51"));
        seeds.add(row("TGG2", DayOfWeek.THU, SessionType.THEORY, "18:00", "18:50"));

        // THEORY - FRI
        seeds.add(row("TCC1", DayOfWeek.FRI, SessionType.THEORY, "08:00", "08:50"));
        seeds.add(row("TB1", DayOfWeek.FRI, SessionType.THEORY, "09:00", "09:50"));
        seeds.add(row("TA1", DayOfWeek.FRI, SessionType.THEORY, "10:00", "10:50"));
        seeds.add(row("F1", DayOfWeek.FRI, SessionType.THEORY, "11:00", "11:50"));
        seeds.add(row("TE1", DayOfWeek.FRI, SessionType.THEORY, "12:00", "12:50"));
        seeds.add(row("SD2", DayOfWeek.FRI, SessionType.THEORY, "12:01", "12:51"));
        seeds.add(row("C2", DayOfWeek.FRI, SessionType.THEORY, "14:00", "14:50"));
        seeds.add(row("TB2", DayOfWeek.FRI, SessionType.THEORY, "15:00", "15:50"));
        seeds.add(row("TA2", DayOfWeek.FRI, SessionType.THEORY, "16:00", "16:50"));
        seeds.add(row("F2", DayOfWeek.FRI, SessionType.THEORY, "17:00", "17:50"));
        seeds.add(row("TEE2", DayOfWeek.FRI, SessionType.THEORY, "18:00", "18:50"));

        // THEORY - SAT
        seeds.add(row("TDD1", DayOfWeek.SAT, SessionType.THEORY, "08:00", "08:50"));
        seeds.add(row("E1", DayOfWeek.SAT, SessionType.THEORY, "09:00", "09:50"));
        seeds.add(row("SE2", DayOfWeek.SAT, SessionType.THEORY, "09:01", "09:51"));
        seeds.add(row("C1", DayOfWeek.SAT, SessionType.THEORY, "10:00", "10:50"));
        seeds.add(row("TF1", DayOfWeek.SAT, SessionType.THEORY, "11:00", "11:50"));
        seeds.add(row("G1", DayOfWeek.SAT, SessionType.THEORY, "11:01", "11:51"));
        seeds.add(row("A1", DayOfWeek.SAT, SessionType.THEORY, "12:00", "12:50"));
        seeds.add(row("D2", DayOfWeek.SAT, SessionType.THEORY, "14:00", "14:50"));
        seeds.add(row("E2", DayOfWeek.SAT, SessionType.THEORY, "15:00", "15:50"));
        seeds.add(row("SD1", DayOfWeek.SAT, SessionType.THEORY, "15:01", "15:51"));
        seeds.add(row("TAA2", DayOfWeek.SAT, SessionType.THEORY, "16:00", "16:50"));
        seeds.add(row("ECS", DayOfWeek.SAT, SessionType.THEORY, "16:01", "16:51"));
        seeds.add(row("TBB2", DayOfWeek.SAT, SessionType.THEORY, "17:00", "17:50"));
        seeds.add(row("CLUB", DayOfWeek.SAT, SessionType.THEORY, "17:01", "17:51"));
        seeds.add(row("TFF2", DayOfWeek.SAT, SessionType.THEORY, "18:00", "18:50"));

        // LAB - TUE
        seeds.add(row("L1", DayOfWeek.TUE, SessionType.LAB, "08:00", "08:50"));
        seeds.add(row("L2", DayOfWeek.TUE, SessionType.LAB, "09:00", "09:50"));
        seeds.add(row("L3", DayOfWeek.TUE, SessionType.LAB, "09:50", "10:40"));
        seeds.add(row("L4", DayOfWeek.TUE, SessionType.LAB, "11:00", "11:50"));
        seeds.add(row("L5", DayOfWeek.TUE, SessionType.LAB, "11:50", "12:40"));
        seeds.add(row("L6", DayOfWeek.TUE, SessionType.LAB, "12:40", "13:30"));
        seeds.add(row("L31", DayOfWeek.TUE, SessionType.LAB, "14:00", "14:50"));
        seeds.add(row("L32", DayOfWeek.TUE, SessionType.LAB, "14:50", "15:40"));
        seeds.add(row("L33", DayOfWeek.TUE, SessionType.LAB, "16:00", "16:50"));
        seeds.add(row("L34", DayOfWeek.TUE, SessionType.LAB, "16:50", "17:40"));
        seeds.add(row("L35", DayOfWeek.TUE, SessionType.LAB, "18:00", "18:50"));
        seeds.add(row("L36", DayOfWeek.TUE, SessionType.LAB, "18:50", "19:30"));

        // LAB - WED
        seeds.add(row("L7", DayOfWeek.WED, SessionType.LAB, "08:00", "08:50"));
        seeds.add(row("L8", DayOfWeek.WED, SessionType.LAB, "09:00", "09:50"));
        seeds.add(row("L9", DayOfWeek.WED, SessionType.LAB, "09:50", "10:40"));
        seeds.add(row("L10", DayOfWeek.WED, SessionType.LAB, "11:00", "11:50"));
        seeds.add(row("L11", DayOfWeek.WED, SessionType.LAB, "11:50", "12:40"));
        seeds.add(row("L12", DayOfWeek.WED, SessionType.LAB, "12:40", "13:30"));
        seeds.add(row("L37", DayOfWeek.WED, SessionType.LAB, "14:00", "14:50"));
        seeds.add(row("L38", DayOfWeek.WED, SessionType.LAB, "14:50", "15:40"));
        seeds.add(row("L39", DayOfWeek.WED, SessionType.LAB, "16:00", "16:50"));
        seeds.add(row("L40", DayOfWeek.WED, SessionType.LAB, "16:50", "17:40"));
        seeds.add(row("L41", DayOfWeek.WED, SessionType.LAB, "18:00", "18:50"));
        seeds.add(row("L42", DayOfWeek.WED, SessionType.LAB, "18:50", "19:30"));

        // LAB - THU
        seeds.add(row("L13", DayOfWeek.THU, SessionType.LAB, "08:00", "08:50"));
        seeds.add(row("L14", DayOfWeek.THU, SessionType.LAB, "09:00", "09:50"));
        seeds.add(row("L15", DayOfWeek.THU, SessionType.LAB, "09:50", "10:40"));
        seeds.add(row("L16", DayOfWeek.THU, SessionType.LAB, "11:00", "11:50"));
        seeds.add(row("L17", DayOfWeek.THU, SessionType.LAB, "11:50", "12:40"));
        seeds.add(row("L18", DayOfWeek.THU, SessionType.LAB, "12:40", "13:30"));
        seeds.add(row("L43", DayOfWeek.THU, SessionType.LAB, "14:00", "14:50"));
        seeds.add(row("L44", DayOfWeek.THU, SessionType.LAB, "14:50", "15:40"));
        seeds.add(row("L45", DayOfWeek.THU, SessionType.LAB, "16:00", "16:50"));
        seeds.add(row("L46", DayOfWeek.THU, SessionType.LAB, "16:50", "17:40"));
        seeds.add(row("L47", DayOfWeek.THU, SessionType.LAB, "18:00", "18:50"));
        seeds.add(row("L48", DayOfWeek.THU, SessionType.LAB, "18:50", "19:30"));

        // LAB - FRI
        seeds.add(row("L19", DayOfWeek.FRI, SessionType.LAB, "08:00", "08:50"));
        seeds.add(row("L20", DayOfWeek.FRI, SessionType.LAB, "09:00", "09:50"));
        seeds.add(row("L21", DayOfWeek.FRI, SessionType.LAB, "09:50", "10:40"));
        seeds.add(row("L22", DayOfWeek.FRI, SessionType.LAB, "11:00", "11:50"));
        seeds.add(row("L23", DayOfWeek.FRI, SessionType.LAB, "11:50", "12:40"));
        seeds.add(row("L24", DayOfWeek.FRI, SessionType.LAB, "12:40", "13:30"));
        seeds.add(row("L49", DayOfWeek.FRI, SessionType.LAB, "14:00", "14:50"));
        seeds.add(row("L50", DayOfWeek.FRI, SessionType.LAB, "14:50", "15:40"));
        seeds.add(row("L51", DayOfWeek.FRI, SessionType.LAB, "16:00", "16:50"));
        seeds.add(row("L52", DayOfWeek.FRI, SessionType.LAB, "16:50", "17:40"));
        seeds.add(row("L53", DayOfWeek.FRI, SessionType.LAB, "18:00", "18:50"));
        seeds.add(row("L54", DayOfWeek.FRI, SessionType.LAB, "18:50", "19:30"));

        // LAB - SAT
        seeds.add(row("L25", DayOfWeek.SAT, SessionType.LAB, "08:00", "08:50"));
        seeds.add(row("L26", DayOfWeek.SAT, SessionType.LAB, "09:00", "09:50"));
        seeds.add(row("L27", DayOfWeek.SAT, SessionType.LAB, "09:50", "10:40"));
        seeds.add(row("L28", DayOfWeek.SAT, SessionType.LAB, "11:00", "11:50"));
        seeds.add(row("L29", DayOfWeek.SAT, SessionType.LAB, "11:50", "12:40"));
        seeds.add(row("L30", DayOfWeek.SAT, SessionType.LAB, "12:40", "13:30"));
        seeds.add(row("L55", DayOfWeek.SAT, SessionType.LAB, "14:00", "14:50"));
        seeds.add(row("L56", DayOfWeek.SAT, SessionType.LAB, "14:50", "15:40"));
        seeds.add(row("L57", DayOfWeek.SAT, SessionType.LAB, "16:00", "16:50"));
        seeds.add(row("L58", DayOfWeek.SAT, SessionType.LAB, "16:50", "17:40"));
        seeds.add(row("L59", DayOfWeek.SAT, SessionType.LAB, "18:00", "18:50"));
        seeds.add(row("L60", DayOfWeek.SAT, SessionType.LAB, "18:50", "19:30"));

        return seeds;
    }

    private MasterTimetableSlot row(String slotName, DayOfWeek dayOfWeek, SessionType sessionType, String start, String end) {
        MasterTimetableSlot slot = new MasterTimetableSlot();
        slot.setSlotName(slotName);
        slot.setDayOfWeek(dayOfWeek);
        slot.setSessionType(sessionType);
        slot.setStartTime(LocalTime.parse(start));
        slot.setEndTime(LocalTime.parse(end));
        return slot;
    }

    private String key(String slotName, DayOfWeek dayOfWeek, SessionType sessionType, LocalTime start, LocalTime end) {
        return slotName + "|" + dayOfWeek + "|" + sessionType + "|" + start + "|" + end;
    }
}
