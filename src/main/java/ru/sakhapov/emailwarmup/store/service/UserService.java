package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sakhapov.emailwarmup.api.dto.UserMeResponse;
import ru.sakhapov.emailwarmup.store.entity.User;
import ru.sakhapov.emailwarmup.store.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserMeResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(role -> role.getName().name()).sorted().toList())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
