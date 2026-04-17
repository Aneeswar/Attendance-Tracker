package com.deepak.Attendance.service;

import com.deepak.Attendance.entity.MasterTimetableSlot;
import com.deepak.Attendance.repository.MasterTimetableSlotRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MasterTimetableService {

    private final MasterTimetableSlotRepository masterTimetableSlotRepository;

    public MasterTimetableService(MasterTimetableSlotRepository masterTimetableSlotRepository) {
        this.masterTimetableSlotRepository = masterTimetableSlotRepository;
    }

    public List<MasterTimetableSlot> findBySlotName(String slotName) {
        if (slotName == null || slotName.trim().isEmpty()) {
            return List.of();
        }

        List<MasterTimetableSlot> rows = masterTimetableSlotRepository.findBySlotNameIgnoreCase(slotName.trim());
        return sortRows(rows);
    }

    public List<MasterTimetableSlot> findBySlotExpression(String slotExpression) {
        List<String> slotNames = parseSlotExpression(slotExpression);
        if (slotNames.isEmpty()) {
            return List.of();
        }

        List<MasterTimetableSlot> rows = masterTimetableSlotRepository.findBySlotNamesUpperIn(slotNames);
        return sortRows(rows);
    }

    public Map<String, List<MasterTimetableSlot>> findGroupedBySlotExpression(String slotExpression) {
        List<MasterTimetableSlot> rows = findBySlotExpression(slotExpression);

        return rows.stream().collect(Collectors.groupingBy(
                MasterTimetableSlot::getSlotName,
                Collectors.collectingAndThen(Collectors.toList(), this::sortRows)
        ));
    }

    private List<String> parseSlotExpression(String slotExpression) {
        if (slotExpression == null || slotExpression.trim().isEmpty()) {
            return List.of();
        }

        String[] pieces = slotExpression.split("\\+");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String piece : pieces) {
            String normalized = piece == null ? "" : piece.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                unique.add(normalized);
            }
        }
        return new ArrayList<>(unique);
    }

    private List<MasterTimetableSlot> sortRows(Collection<MasterTimetableSlot> rows) {
        return rows.stream()
                .sorted(Comparator.comparing(MasterTimetableSlot::getDayOfWeek)
                        .thenComparing(MasterTimetableSlot::getSessionType)
                        .thenComparing(MasterTimetableSlot::getStartTime)
                        .thenComparing(MasterTimetableSlot::getSlotName))
                .toList();
    }
}
