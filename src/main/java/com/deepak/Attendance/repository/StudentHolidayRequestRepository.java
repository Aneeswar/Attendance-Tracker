package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.StudentHolidayRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentHolidayRequestRepository extends JpaRepository<StudentHolidayRequest, Long> {
    List<StudentHolidayRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<StudentHolidayRequest> findByStudentIdAndSemesterIdOrderByCreatedAtDesc(Long studentId, Long semesterId);
    List<StudentHolidayRequest> findByStatusOrderByCreatedAtDesc(StudentHolidayRequest.RequestStatus status);
    List<StudentHolidayRequest> findByStatusAndSemesterIdOrderByCreatedAtDesc(StudentHolidayRequest.RequestStatus status, Long semesterId);
    List<StudentHolidayRequest> findAllByOrderByCreatedAtDesc();
    List<StudentHolidayRequest> findBySemesterIdOrderByCreatedAtDesc(Long semesterId);
    long countBySemesterId(Long semesterId);
    List<StudentHolidayRequest> findByStudentIdNotOrderByCreatedAtDesc(Long studentId);
    List<StudentHolidayRequest> findByStudentIdNotAndSemesterIdOrderByCreatedAtDesc(Long studentId, Long semesterId);
}
