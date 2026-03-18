package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sakhapov.emailwarmup.api.dto.UserMeResponse;
import ru.sakhapov.emailwarmup.store.entity.Role;
import ru.sakhapov.emailwarmup.store.entity.RoleName;
import ru.sakhapov.emailwarmup.store.entity.User;
import ru.sakhapov.emailwarmup.store.repository.UserRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getCurrentUserShouldReturnMappedUserWithSortedRoles() {
        User user = User.builder()
                .id(1L)
                .email("owner@test.com")
                .roles(Set.of(
                        Role.builder().name(RoleName.ROLE_USER).build(),
                        Role.builder().name(RoleName.ROLE_ADMIN).build()
                ))
                .build();
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));

        UserMeResponse response = userService.getCurrentUser("owner@test.com");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("owner@test.com");
        assertThat(response.getRoles()).containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void getCurrentUserShouldFailWhenUserMissing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("missing@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }
}
