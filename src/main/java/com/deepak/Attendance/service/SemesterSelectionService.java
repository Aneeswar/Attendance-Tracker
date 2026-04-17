package com.deepak.Attendance.service;

import com.deepak.Attendance.entity.Semester;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.SemesterRepository;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.security.AuthUserResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SemesterSelectionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private AuthUserResolver authUserResolver;

    @Transactional
    public void updateStudentCurrentSemester(String authHeader, Long semesterId) {
        Long userId = authUserResolver.extractUserIdFromToken(authHeader);
        updateStudentCurrentSemesterByUserId(userId, semesterId);
    }

    @Transactional
    public void updateStudentCurrentSemesterByUserId(Long userId, Long semesterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));

        if (!Boolean.TRUE.equals(semester.getActive())) {
            throw new IllegalArgumentException("Semester is inactive");
        }

        user.setCurrentSemester(semester);
        userRepository.save(user);
    }

    public Optional<Semester> getCurrentSemesterForUser(Long userId) {
        return userRepository.findById(userId)
                .map(User::getCurrentSemester)
                .or(() -> semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc());
    }
}
