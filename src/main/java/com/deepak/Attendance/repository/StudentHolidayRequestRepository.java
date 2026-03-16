package com.deepak.Attendance.repository;

import com.deepak.Attendance.entity.StudentHolidayRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentHolidayRequestRepository extends JpaRepository<StudentHolidayRequest, Long> {
    List<StudentHolidayRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<StudentHolidayRequest> findByStatusOrderByCreatedAtDesc(StudentHolidayRequest.RequestStatus status);
    List<StudentHolidayRequest> findAllByOrderByCreatedAtDesc();
    List<StudentHolidayRequest> findByStudentIdNotOrderByCreatedAtDesc(Long studentId);
}
