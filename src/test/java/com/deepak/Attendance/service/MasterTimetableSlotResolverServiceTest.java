package com.deepak.Attendance.service;

import com.deepak.Attendance.entity.MasterTimetableSlot;
import com.deepak.Attendance.entity.enums.DayOfWeek;
import com.deepak.Attendance.entity.enums.SessionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterTimetableSlotResolverServiceTest {

    @Mock
    private MasterTimetableService masterTimetableService;

    @InjectMocks
    private MasterTimetableSlotResolverService resolverService;

    @Test
    void resolveTokens_returnsAllTheoryRowsForCombinedSlots() {
        MasterTimetableSlot b1Tue = slot("B1", DayOfWeek.TUE, SessionType.THEORY, "10:00", "10:50");
        MasterTimetableSlot b1Wed = slot("B1", DayOfWeek.WED, SessionType.THEORY, "12:00", "12:50");
        MasterTimetableSlot tb1Fri = slot("TB1", DayOfWeek.FRI, SessionType.THEORY, "09:00", "09:50");

        when(masterTimetableService.findBySlotExpression("B1+TB1")).thenReturn(List.of(b1Tue, b1Wed, tb1Fri));

        MasterTimetableSlotResolverService.ResolutionResult result = resolverService.resolveTokens(List.of("B1", "TB1"), SessionType.THEORY);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(3, result.getRows().size());
    }

    @Test
    void resolveTokens_rejectsTypeMismatch() {
        MasterTimetableSlot l33 = slot("L33", DayOfWeek.TUE, SessionType.LAB, "16:00", "16:50");
        when(masterTimetableService.findBySlotExpression("L33")).thenReturn(List.of(l33));

        MasterTimetableSlotResolverService.ResolutionResult result = resolverService.resolveTokens(List.of("L33"), SessionType.THEORY);

        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getRows().isEmpty());
    }

        @Test
        void resolveTokens_forF1PlusTF1_returnsSingleFridayTf1Only() {
        MasterTimetableSlot f1Wed = slot("F1", DayOfWeek.WED, SessionType.THEORY, "10:00", "10:50");
        MasterTimetableSlot f1Fri = slot("F1", DayOfWeek.FRI, SessionType.THEORY, "11:00", "11:50");
            MasterTimetableSlot tf1Sat = slot("TF1", DayOfWeek.SAT, SessionType.THEORY, "11:00", "11:50");

        when(masterTimetableService.findBySlotExpression("F1+TF1"))
                    .thenReturn(List.of(f1Wed, f1Fri, tf1Sat));

        MasterTimetableSlotResolverService.ResolutionResult result =
            resolverService.resolveTokens(List.of("F1", "TF1"), SessionType.THEORY);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(3, result.getRows().size());

        long tf1Count = result.getRows().stream()
            .filter(r -> "TF1".equals(r.getSlotName()))
            .count();
        assertEquals(1, tf1Count);

        assertTrue(result.getRows().stream().anyMatch(r ->
            "TF1".equals(r.getSlotName())
                && r.getDayOfWeek() == DayOfWeek.SAT
                && r.getStartTime().equals(LocalTime.of(11, 0))
                && r.getEndTime().equals(LocalTime.of(11, 50))
        ));
        }

    private MasterTimetableSlot slot(String slotName, DayOfWeek day, SessionType type, String start, String end) {
        MasterTimetableSlot slot = new MasterTimetableSlot();
        slot.setSlotName(slotName);
        slot.setDayOfWeek(day);
        slot.setSessionType(type);
        slot.setStartTime(LocalTime.parse(start));
        slot.setEndTime(LocalTime.parse(end));
        return slot;
    }
}
