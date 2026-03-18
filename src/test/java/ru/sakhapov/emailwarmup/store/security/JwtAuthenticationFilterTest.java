package ru.sakhapov.emailwarmup.store.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPassThroughWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldAuthenticateWhenJwtIsValid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        UserDetails userDetails = User.withUsername("owner@test.com")
                .password("encoded")
                .authorities("ROLE_USER")
                .build();

        when(jwtService.extractUsername("valid-jwt")).thenReturn("owner@test.com");
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-jwt", userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("owner@test.com");
    }

    @Test
    void shouldNotAuthenticateWhenJwtIsInvalid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        UserDetails userDetails = User.withUsername("owner@test.com")
                .password("encoded")
                .authorities("ROLE_USER")
                .build();

        when(jwtService.extractUsername("invalid-jwt")).thenReturn("owner@test.com");
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid-jwt", userDetails)).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
