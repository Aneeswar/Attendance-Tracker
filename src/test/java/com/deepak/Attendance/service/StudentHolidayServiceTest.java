package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.StudentHolidayRequestDTO;
import com.deepak.Attendance.entity.StudentHolidayRequest;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import com.deepak.Attendance.repository.StudentHolidayRequestRepository;
import com.deepak.Attendance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentHolidayServiceTest {

    @Mock
    private StudentHolidayRequestRepository requestRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcademicCalendarRepository academicCalendarRepository;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentHolidayService studentHolidayService;

    @Test
    void createRequest_throwsWhenStudentMissing() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        StudentHolidayRequestDTO dto = new StudentHolidayRequestDTO();
        dto.setHolidayDate(LocalDate.now());
        dto.setReason("Medical leave");

        assertThrows(RuntimeException.class, () -> studentHolidayService.createRequest(7L, dto));
    }

    @Test
    void updateRequest_throwsWhenRequestBelongsToAnotherUser() {
        User owner = new User();
        owner.setId(21L);

        StudentHolidayRequest req = new StudentHolidayRequest();
        req.setId(5L);
        req.setStudent(owner);
        req.setStatus(StudentHolidayRequest.RequestStatus.PENDING);

        when(requestRepository.findById(5L)).thenReturn(Optional.of(req));

        StudentHolidayRequestDTO dto = new StudentHolidayRequestDTO();
        dto.setHolidayDate(LocalDate.now().plusDays(1));
        dto.setReason("Update");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> studentHolidayService.updateRequest(9L, 5L, dto));

        assertEquals("Unauthorized: This request does not belong to you", ex.getMessage());
    }

    @Test
    void getAllRequests_mapsSavedEntityToDto() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student1");

        StudentHolidayRequest req = new StudentHolidayRequest();
        req.setId(10L);
        req.setStudent(user);
        req.setReason("Event");
        req.setHolidayDate(LocalDate.of(2026, 3, 20));

        when(requestRepository.save(any(StudentHolidayRequest.class))).thenReturn(req);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        StudentHolidayRequestDTO in = new StudentHolidayRequestDTO();
        in.setHolidayDate(LocalDate.of(2026, 3, 20));
        in.setReason("Event");

        StudentHolidayRequestDTO out = studentHolidayService.createRequest(1L, in);
        assertEquals(10L, out.getId());
        assertEquals("student1", out.getStudentName());
    }
}
