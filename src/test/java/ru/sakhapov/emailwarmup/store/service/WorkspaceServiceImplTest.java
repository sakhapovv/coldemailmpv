package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sakhapov.emailwarmup.api.dto.WorkspaceMemberResponse;
import ru.sakhapov.emailwarmup.api.dto.WorkspaceResponse;
import ru.sakhapov.emailwarmup.store.entity.User;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.entity.WorkspaceMember;
import ru.sakhapov.emailwarmup.store.entity.WorkspaceRole;
import ru.sakhapov.emailwarmup.store.repository.UserRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceMemberRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceImplTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    @Test
    void getCurrentWorkspaceShouldReturnMappedWorkspace() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        Workspace workspace = Workspace.builder()
                .id(10L)
                .name("Personal workspace")
                .owner(owner)
                .build();
        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));

        WorkspaceResponse response = workspaceService.getCurrentWorkspace("owner@test.com");

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Personal workspace");
        assertThat(response.getOwnerUserId()).isEqualTo(1L);
    }

    @Test
    void addMemberShouldCreateNewMemberWhenMissing() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        Workspace workspace = Workspace.builder().id(10L).owner(owner).name("Personal workspace").build();
        User invitee = User.builder().id(2L).email("member@test.com").build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(invitee));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L)).thenReturn(Optional.empty());
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> {
            WorkspaceMember member = invocation.getArgument(0);
            member.setId(20L);
            return member;
        });

        WorkspaceMemberResponse response = workspaceService.addMemberToCurrentWorkspace("owner@test.com", "member@test.com");

        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getEmail()).isEqualTo("member@test.com");
        assertThat(response.getRole()).isEqualTo("MEMBER");
    }

    @Test
    void addMemberShouldReturnExistingMembership() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        Workspace workspace = Workspace.builder().id(10L).owner(owner).build();
        User invitee = User.builder().id(2L).email("member@test.com").build();
        WorkspaceMember member = WorkspaceMember.builder()
                .id(20L)
                .workspace(workspace)
                .user(invitee)
                .role(WorkspaceRole.MEMBER)
                .build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(invitee));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));

        WorkspaceMemberResponse response = workspaceService.addMemberToCurrentWorkspace("owner@test.com", "member@test.com");

        assertThat(response.getUserId()).isEqualTo(2L);
        verify(workspaceMemberRepository, never()).save(any());
    }

    @Test
    void getCurrentWorkspaceMembersShouldReturnMappedMembers() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        Workspace workspace = Workspace.builder().id(10L).owner(owner).build();
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .build();
        User memberUser = User.builder().id(2L).email("member@test.com").build();
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(memberUser)
                .role(WorkspaceRole.MEMBER)
                .build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findAllByWorkspaceIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(ownerMember, member));

        List<WorkspaceMemberResponse> responses = workspaceService.getCurrentWorkspaceMembers("owner@test.com");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(WorkspaceMemberResponse::getRole)
                .containsExactly("OWNER", "MEMBER");
    }

    @Test
    void removeMemberShouldDeleteNonOwner() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        Workspace workspace = Workspace.builder().id(10L).owner(owner).build();
        User memberUser = User.builder().id(2L).email("member@test.com").build();
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(memberUser)
                .role(WorkspaceRole.MEMBER)
                .build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));

        workspaceService.removeMemberFromCurrentWorkspace("owner@test.com", 2L);

        verify(workspaceMemberRepository).delete(member);
    }

    @Test
    void removeMemberShouldRejectOwner() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        Workspace workspace = Workspace.builder().id(10L).owner(owner).build();
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> workspaceService.removeMemberFromCurrentWorkspace("owner@test.com", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Owner cannot be removed");
    }
}
