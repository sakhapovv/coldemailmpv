package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sakhapov.emailwarmup.api.dto.AuthResponse;
import ru.sakhapov.emailwarmup.api.dto.LoginRequest;
import ru.sakhapov.emailwarmup.api.dto.RegisterRequest;
import ru.sakhapov.emailwarmup.store.entity.Role;
import ru.sakhapov.emailwarmup.store.entity.RoleName;
import ru.sakhapov.emailwarmup.store.entity.User;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.entity.WorkspaceMember;
import ru.sakhapov.emailwarmup.store.entity.WorkspaceRole;
import ru.sakhapov.emailwarmup.store.repository.RoleRepository;
import ru.sakhapov.emailwarmup.store.repository.UserRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceMemberRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;
import ru.sakhapov.emailwarmup.store.security.JwtService;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_USER).build()));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);
        Workspace workspace = workspaceRepository.save(
                Workspace.builder()
                        .name("Personal workspace")
                        .owner(user)
                        .build()
        );
        workspaceMemberRepository.save(
                WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(user)
                        .role(WorkspaceRole.OWNER)
                        .build()
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwt = jwtService.generateToken(userDetails);
        return AuthResponse.builder().token(jwt).build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String jwt = jwtService.generateToken(userDetails);
        return AuthResponse.builder().token(jwt).build();
    }
}
