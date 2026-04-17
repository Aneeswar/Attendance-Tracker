package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.ResolvedScheduleRowDTO;
import com.deepak.Attendance.entity.MasterTimetableSlot;
import com.deepak.Attendance.entity.enums.SessionType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MasterTimetableSlotResolverService {

    private final MasterTimetableService masterTimetableService;

    public MasterTimetableSlotResolverService(MasterTimetableService masterTimetableService) {
        this.masterTimetableService = masterTimetableService;
    }

    public ResolutionResult resolveTokens(List<String> tokens, SessionType expectedType) {
        ResolutionResult result = new ResolutionResult();
        if (tokens == null || tokens.isEmpty()) {
            result.getErrors().add("slotString must contain at least one token");
            return result;
        }

        List<MasterTimetableSlot> allRows = masterTimetableService.findBySlotExpression(String.join("+", tokens));
        Map<String, List<MasterTimetableSlot>> bySlot = allRows.stream()
                .collect(Collectors.groupingBy(s -> s.getSlotName().toUpperCase()));

        for (String token : tokens) {
            List<MasterTimetableSlot> tokenRows = bySlot.get(token.toUpperCase());
            if (tokenRows == null || tokenRows.isEmpty()) {
                result.getErrors().add("Slot token '" + token + "' does not exist in master timetable");
                continue;
            }

            List<MasterTimetableSlot> mismatched = tokenRows.stream()
                    .filter(s -> s.getSessionType() != expectedType)
                    .toList();

            if (!mismatched.isEmpty()) {
                result.getErrors().add("Slot token '" + token + "' is not valid for type " + expectedType);
                continue;
            }

            result.getRows().addAll(tokenRows.stream().map(this::toDto).toList());
        }

        result.setRows(uniqueSorted(result.getRows()));
        return result;
    }

    private List<ResolvedScheduleRowDTO> uniqueSorted(Collection<ResolvedScheduleRowDTO> rows) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<ResolvedScheduleRowDTO> deduped = new ArrayList<>();

        for (ResolvedScheduleRowDTO row : rows) {
            String key = row.getSlotName() + "|" + row.getDayOfWeek() + "|" + row.getSessionType() + "|" + row.getStartTime() + "|" + row.getEndTime();
            if (seen.add(key)) {
                deduped.add(row);
            }
        }

        return deduped.stream()
                .sorted(Comparator.comparing(ResolvedScheduleRowDTO::getDayOfWeek)
                        .thenComparing(ResolvedScheduleRowDTO::getStartTime)
                        .thenComparing(ResolvedScheduleRowDTO::getSlotName))
                .toList();
    }

    private ResolvedScheduleRowDTO toDto(MasterTimetableSlot slot) {
        return new ResolvedScheduleRowDTO(
                slot.getSlotName(),
                slot.getDayOfWeek(),
                slot.getSessionType(),
                slot.getStartTime(),
                slot.getEndTime()
        );
    }

    public static class ResolutionResult {
        private List<ResolvedScheduleRowDTO> rows = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public List<ResolvedScheduleRowDTO> getRows() {
            return rows;
        }

        public void setRows(List<ResolvedScheduleRowDTO> rows) {
            this.rows = rows;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
}
