package com.saadaoui.tunidesk.auth.service.impl;

import com.saadaoui.tunidesk.audit.service.AuditLogService;
import com.saadaoui.tunidesk.auth.entity.Role;
import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.auth.repository.RoleRepository;
import com.saadaoui.tunidesk.auth.repository.UserRepository;
import com.saadaoui.tunidesk.exception.AccessDeniedException;
import com.saadaoui.tunidesk.exception.DuplicateResourceException;
import com.saadaoui.tunidesk.user.dto.ChangePasswordRequest;
import com.saadaoui.tunidesk.user.dto.CreateUserRequest;
import com.saadaoui.tunidesk.user.dto.ProfileUpdateRequest;
import com.saadaoui.tunidesk.user.dto.UpdateUserRequest;
import com.saadaoui.tunidesk.user.dto.UserResponse;
import com.saadaoui.tunidesk.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuditLogService auditLogService;

    private UserServiceImpl service;

    private Role adminRole;

    private Role agentRole;

    private Role userRole;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                userRepository, roleRepository, passwordEncoder, userMapper, auditLogService);

        adminRole = Role.builder().name("ADMIN").build();
        agentRole = Role.builder().name("AGENT").build();
        userRole = Role.builder().name("USER").build();
    }

    @Test
    void createUser_duplicateEmail_shouldThrow() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("Dup");
        request.setLastName("User");
        request.setEmail("dup@test.com");
        request.setPassword("secret1");

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createUser_success_shouldEncodePasswordAndAssignRole() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("$2a$encoded");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().build());

        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("New");
        request.setLastName("User");
        request.setEmail("new@test.com");
        request.setPassword("secret1");

        service.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("$2a$encoded");
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("USER");
    }

    @Test
    void updateUser_duplicateEmailOnOtherAccount_shouldThrow() {
        User target = User.builder().id(UUID.randomUUID()).email("keep@test.com")
                .roles(Set.of(userRole)).build();

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.existsByEmailAndIdNot("taken@test.com", target.getId())).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Keep");
        request.setLastName("User");
        request.setEmail("taken@test.com");
        request.setEnabled(true);
        request.setRoles(List.of("USER"));

        assertThatThrownBy(() -> service.updateUser(target.getId(), request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateUser_disableLastAdmin_shouldThrow() {
        User target = User.builder().id(UUID.randomUUID()).email("admin@test.com")
                .roles(Set.of(adminRole)).build();

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.existsByEmailAndIdNot(anyString(), any(UUID.class))).thenReturn(false);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.countByEnabledTrueAndRolesName("ADMIN")).thenReturn(1L);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Admin");
        request.setLastName("Test");
        request.setEmail("admin@test.com");
        request.setEnabled(false);
        request.setRoles(List.of("ADMIN"));

        assertThatThrownBy(() -> service.updateUser(target.getId(), request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateUser_success_shouldPersistChanges() {
        User target = User.builder().id(UUID.randomUUID()).firstName("Old").lastName("Name")
                .email("old@test.com").roles(Set.of(userRole)).build();

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.existsByEmailAndIdNot(anyString(), any(UUID.class))).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().build());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("New");
        request.setLastName("Name");
        request.setEmail("old@test.com");
        request.setPhoneNumber("+216 55 123 456");
        request.setEnabled(true);
        request.setRoles(List.of("USER"));

        service.updateUser(target.getId(), request);

        assertThat(target.getFirstName()).isEqualTo("New");
        assertThat(target.getPhoneNumber()).isEqualTo("+216 55 123 456");
    }

    @Test
    void deleteUser_ownAccount_shouldThrow() {
        User target = User.builder().id(UUID.randomUUID()).email("me@test.com")
                .roles(Set.of(userRole)).build();

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.deleteUser(target.getId(), "me@test.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteUser_otherAccount_shouldDelete() {
        User target = User.builder().id(UUID.randomUUID()).email("other@test.com")
                .roles(Set.of(userRole)).build();

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        service.deleteUser(target.getId(), "admin@test.com");

        verify(userRepository).deleteById(target.getId());
        verify(auditLogService).record(anyString(), anyString(), any(UUID.class), anyString());
    }

    @Test
    void updateProfile_shouldPersistChanges() {
        User target = User.builder().id(UUID.randomUUID()).firstName("Old").lastName("Name")
                .email("me@test.com").phoneNumber("+2161").roles(Set.of(userRole)).build();

        when(userRepository.findByEmail("me@test.com")).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().build());

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName("New");
        request.setLastName("Name");
        request.setPhoneNumber("+216 22 111 222");

        service.updateProfile("me@test.com", request);

        assertThat(target.getFirstName()).isEqualTo("New");
        assertThat(target.getPhoneNumber()).isEqualTo("+216 22 111 222");
        verify(auditLogService).record(anyString(), anyString(), any(UUID.class), anyString());
    }

    @Test
    void changePassword_wrongCurrentPassword_shouldThrow() {
        User target = User.builder().id(UUID.randomUUID()).email("me@test.com")
                .password("$2a$encoded").roles(Set.of(userRole)).build();

        when(userRepository.findByEmail("me@test.com")).thenReturn(Optional.of(target));
        when(passwordEncoder.matches("wrong", "$2a$encoded")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("newSecret1");

        assertThatThrownBy(() -> service.changePassword("me@test.com", request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePassword_validCurrentPassword_shouldEncodeAndSave() {
        User target = User.builder().id(UUID.randomUUID()).email("me@test.com")
                .password("$2a$encoded").roles(Set.of(userRole)).build();

        when(userRepository.findByEmail("me@test.com")).thenReturn(Optional.of(target));
        when(passwordEncoder.matches("oldPass1", "$2a$encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPass1")).thenReturn("$2a$new");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass1");
        request.setNewPassword("newPass1");

        service.changePassword("me@test.com", request);

        assertThat(target.getPassword()).isEqualTo("$2a$new");
        verify(auditLogService).record(anyString(), anyString(), any(UUID.class), anyString());
    }
}
