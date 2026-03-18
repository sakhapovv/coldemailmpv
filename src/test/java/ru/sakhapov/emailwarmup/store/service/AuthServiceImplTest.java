package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.sakhapov.emailwarmup.api.dto.AuthResponse;
import ru.sakhapov.emailwarmup.api.dto.LoginRequest;
import ru.sakhapov.emailwarmup.api.dto.RegisterRequest;
import ru.sakhapov.emailwarmup.store.entity.Role;
import ru.sakhapov.emailwarmup.store.entity.RoleName;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.entity.WorkspaceMember;
import ru.sakhapov.emailwarmup.store.entity.WorkspaceRole;
import ru.sakhapov.emailwarmup.store.repository.RoleRepository;
import ru.sakhapov.emailwarmup.store.repository.UserRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceMemberRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;
import ru.sakhapov.emailwarmup.store.security.JwtService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerShouldCreateUserWorkspaceAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("owner@test.com");
        request.setPassword("12345678");

        Role userRole = Role.builder().id(10L).name(RoleName.ROLE_USER).build();
        UserDetails userDetails = User.withUsername("owner@test.com")
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();

        when(userRepository.existsByEmail("owner@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("12345678")).thenReturn("encoded-password");
        when(userRepository.save(any(ru.sakhapov.emailwarmup.store.entity.User.class)))
                .thenAnswer(invocation -> {
                    ru.sakhapov.emailwarmup.store.entity.User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace workspace = invocation.getArgument(0);
            workspace.setId(100L);
            return workspace;
        });
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");

        ArgumentCaptor<ru.sakhapov.emailwarmup.store.entity.User> userCaptor =
                ArgumentCaptor.forClass(ru.sakhapov.emailwarmup.store.entity.User.class);
        verify(userRepository).save(userCaptor.capture());
        ru.sakhapov.emailwarmup.store.entity.User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("owner@test.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getRoles()).containsExactly(userRole);

        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(memberCaptor.getValue().getUser().getEmail()).isEqualTo("owner@test.com");
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("owner@test.com");
        request.setPassword("12345678");

        when(userRepository.existsByEmail("owner@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginShouldAuthenticateAndReturnToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("owner@test.com");
        request.setPassword("12345678");

        UserDetails userDetails = User.withUsername("owner@test.com")
                .password("encoded")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("owner@test.com", "12345678")
        );
    }

    @Test
    void loginShouldPropagateBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("owner@test.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");
    }
}
