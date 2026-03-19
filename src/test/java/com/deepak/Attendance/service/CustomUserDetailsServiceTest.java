package com.deepak.Attendance.service;

import com.deepak.Attendance.entity.Role;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_returnsUserDetailsWhenFound() {
        User user = new User();
        user.setUsername("student1");
        user.setPassword("encoded-pass");
        user.setEnabled(true);
        user.setRoles(Set.of(new Role("STUDENT")));

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("student1");

        assertEquals("student1", details.getUsername());
        assertTrue(details.isEnabled());
        assertEquals(1, details.getAuthorities().size());
    }

    @Test
    void loadUserByUsername_throwsWhenNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing"));
    }
}
