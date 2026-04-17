package com.deepak.Attendance.config;

import com.deepak.Attendance.entity.MasterTimetableSlot;
import com.deepak.Attendance.entity.enums.DayOfWeek;
import com.deepak.Attendance.repository.MasterTimetableSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterTimetableSeederTest {

    @Mock
    private MasterTimetableSlotRepository masterTimetableSlotRepository;

    @InjectMocks
    private MasterTimetableSeeder masterTimetableSeeder;

    @Test
    void run_insertsCanonicalRowsWithSingleSaturdayTf1Only() {
        when(masterTimetableSlotRepository.findAll()).thenReturn(List.of());
        when(masterTimetableSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        masterTimetableSeeder.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MasterTimetableSlot>> captor = ArgumentCaptor.forClass(List.class);
        verify(masterTimetableSlotRepository).saveAll(captor.capture());

        List<MasterTimetableSlot> saved = captor.getValue();

        List<MasterTimetableSlot> tf1Rows = saved.stream()
                .filter(r -> "TF1".equals(r.getSlotName()))
                .toList();

        assertEquals(1, tf1Rows.size());
        assertEquals(DayOfWeek.SAT, tf1Rows.get(0).getDayOfWeek());
        assertEquals(LocalTime.of(11, 0), tf1Rows.get(0).getStartTime());
        assertEquals(LocalTime.of(11, 50), tf1Rows.get(0).getEndTime());

        boolean hasSaturdayG1At1101 = saved.stream().anyMatch(r ->
                "G1".equals(r.getSlotName())
                        && r.getDayOfWeek() == DayOfWeek.SAT
                && LocalTime.of(11, 1).equals(r.getStartTime())
                && LocalTime.of(11, 51).equals(r.getEndTime())
        );
        assertTrue(hasSaturdayG1At1101);
    }
}
