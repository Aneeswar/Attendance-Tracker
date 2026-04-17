package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.StudentHolidayRequestDTO;
import com.deepak.Attendance.entity.Holiday;
import com.deepak.Attendance.entity.Semester;
import com.deepak.Attendance.entity.StudentHolidayRequest;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.HolidayRepository;
import com.deepak.Attendance.repository.SemesterRepository;
import com.deepak.Attendance.repository.StudentHolidayRequestRepository;
import com.deepak.Attendance.repository.UserRepository;
// import com.deepak.Attendance.repository.AcademicCalendarRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StudentHolidayService {

    @Autowired
    private StudentHolidayRequestRepository requestRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private UserRepository userRepository;

    // @Autowired
    // private AcademicCalendarRepository academicCalendarRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SemesterRepository semesterRepository;

    public List<StudentHolidayRequestDTO> getRequestsForStudent(Long studentId) {
        Long semesterId = resolveStudentSemesterId(studentId);
        List<StudentHolidayRequest> requests = semesterId != null
            ? requestRepository.findByStudentIdAndSemesterIdOrderByCreatedAtDesc(studentId, semesterId)
            : requestRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        return requests
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudentHolidayRequestDTO> getRequestsByOthers(Long studentId) {
        Long semesterId = resolveStudentSemesterId(studentId);
        List<StudentHolidayRequest> requests = semesterId != null
            ? requestRepository.findByStudentIdNotAndSemesterIdOrderByCreatedAtDesc(studentId, semesterId)
            : requestRepository.findByStudentIdNotOrderByCreatedAtDesc(studentId);
        return requests
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudentHolidayRequestDTO> getAllPendingRequests(Long semesterId) {
        return requestRepository.findByStatusAndSemesterIdOrderByCreatedAtDesc(StudentHolidayRequest.RequestStatus.PENDING, semesterId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudentHolidayRequestDTO> getAllPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtDesc(StudentHolidayRequest.RequestStatus.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudentHolidayRequestDTO> getAllRequests(Long semesterId) {
        if (semesterId == null) {
            return requestRepository.findAllByOrderByCreatedAtDesc()
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
        return requestRepository.findBySemesterIdOrderByCreatedAtDesc(semesterId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudentHolidayRequestDTO> getAllRequests() {
        return requestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentHolidayRequestDTO createRequest(Long studentId, StudentHolidayRequestDTO dto) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        StudentHolidayRequest request = new StudentHolidayRequest();
        request.setStudent(student);
        request.setHolidayDate(dto.getHolidayDate());
        request.setReason(dto.getReason());
        request.setScope(dto.getScope());
        request.setSemester(resolveStudentSemester(student));
        request.setStatus(StudentHolidayRequest.RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        return convertToDTO(requestRepository.save(request));
    }

    @Transactional
    public void deleteRequest(Long studentId, Long requestId) {
        StudentHolidayRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Unauthorized: This request does not belong to you");
        }

        if (request.getStatus() != StudentHolidayRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Cannot delete a request that is already " + request.getStatus());
        }

        requestRepository.delete(request);
    }

    @Transactional
    public StudentHolidayRequestDTO updateRequest(Long studentId, Long requestId, StudentHolidayRequestDTO dto) {
        StudentHolidayRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Unauthorized: This request does not belong to you");
        }

        if (request.getStatus() != StudentHolidayRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Cannot update a request that is already " + request.getStatus());
        }

        request.setHolidayDate(dto.getHolidayDate());
        request.setReason(dto.getReason());
        request.setScope(dto.getScope());
        request.setSemester(resolveStudentSemester(request.getStudent()));
        request.setUpdatedAt(LocalDateTime.now());

        return convertToDTO(requestRepository.save(request));
    }

    @Transactional
    public void approveRequest(Long requestId, String adminComment) {
        StudentHolidayRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != StudentHolidayRequest.RequestStatus.PENDING) {
            return;
        }

        request.setStatus(StudentHolidayRequest.RequestStatus.APPROVED);
        request.setAdminComment(adminComment);
        request.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);

        // Add to main Holidays table
        Semester semester = request.getSemester();
        if (semester == null) {
            semester = resolveStudentSemester(request.getStudent());
        }
        if (semester == null) {
            throw new RuntimeException("No semester configured for this request");
        }
        final Semester finalSemester = semester;

        String approvedReason = "Approved student holiday: " + request.getReason();
        Holiday.HolidayScope requestedScope = request.getScope() != null
                ? request.getScope()
                : Holiday.HolidayScope.FULL;

        Holiday holiday = holidayRepository.findByDateAndSemesterId(request.getHolidayDate(), finalSemester.getId())
                .map(existing -> {
                    if (existing.getAcademicCalendarId() == null) {
                        existing.setAcademicCalendarId(finalSemester.getId());
                    }
                    if (existing.getSemester() == null) {
                        existing.setSemester(finalSemester);
                    }

                    // Merge scopes: MORNING + AFTERNOON on same day should become FULL.
                    Holiday.HolidayScope existingScope = existing.getScope() != null
                            ? existing.getScope()
                            : Holiday.HolidayScope.FULL;
                    if (existingScope == Holiday.HolidayScope.FULL || requestedScope == Holiday.HolidayScope.FULL) {
                        existing.setScope(Holiday.HolidayScope.FULL);
                    } else if (existingScope != requestedScope) {
                        existing.setScope(Holiday.HolidayScope.FULL);
                    }

                    String currentReason = existing.getReason() != null ? existing.getReason() : "";
                    if (!currentReason.contains(approvedReason)) {
                        existing.setReason(currentReason.isBlank() ? approvedReason : currentReason + " | " + approvedReason);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Holiday newHoliday = new Holiday();
                    newHoliday.setAcademicCalendarId(finalSemester.getId());
                    newHoliday.setSemester(finalSemester);
                    newHoliday.setDate(request.getHolidayDate());
                    newHoliday.setReason(approvedReason);
                    newHoliday.setScope(requestedScope);
                    newHoliday.setType(Holiday.HolidayType.EXTRA);
                    return newHoliday;
                });

        holidayRepository.save(holiday);

        // Mark reports as stale to trigger recalculation
        studentService.markAllAttendanceReportsAsStale();
    }

    @Transactional
    public void rejectRequest(Long requestId, String adminComment) {
        StudentHolidayRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != StudentHolidayRequest.RequestStatus.PENDING) {
            return;
        }

        request.setStatus(StudentHolidayRequest.RequestStatus.REJECTED);
        request.setAdminComment(adminComment);
        request.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    private StudentHolidayRequestDTO convertToDTO(StudentHolidayRequest req) {
        StudentHolidayRequestDTO dto = new StudentHolidayRequestDTO();
        dto.setId(req.getId());
        dto.setStudentId(req.getStudent().getId());
        dto.setStudentName(req.getStudent().getUsername());
        dto.setSemesterId(req.getSemester() != null ? req.getSemester().getId() : null);
        dto.setSemesterName(req.getSemester() != null ? req.getSemester().getSemesterName() : null);
        dto.setHolidayDate(req.getHolidayDate());
        dto.setReason(req.getReason());
        dto.setScope(req.getScope());
        dto.setStatus(req.getStatus());
        dto.setAdminComment(req.getAdminComment());
        dto.setCreatedAt(req.getCreatedAt());
        return dto;
    }

    private Long resolveStudentSemesterId(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Semester semester = resolveStudentSemester(student);
        return semester != null ? semester.getId() : null;
    }

    private Semester resolveStudentSemester(User student) {
        if (student.getCurrentSemester() != null) {
            return student.getCurrentSemester();
        }

        if (semesterRepository == null) {
            return null;
        }

        Semester fallback = semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc()
                .orElseThrow(() -> new RuntimeException("No active semester configured"));
        student.setCurrentSemester(fallback);
        userRepository.save(student);
        return fallback;
    }
}
