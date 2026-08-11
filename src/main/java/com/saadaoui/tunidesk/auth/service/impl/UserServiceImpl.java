package com.saadaoui.tunidesk.auth.service.impl;

import com.saadaoui.tunidesk.audit.AuditConstants;
import com.saadaoui.tunidesk.audit.service.AuditLogService;
import com.saadaoui.tunidesk.auth.entity.Role;
import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.auth.repository.RoleRepository;
import com.saadaoui.tunidesk.auth.repository.UserRepository;
import com.saadaoui.tunidesk.auth.service.UserService;
import com.saadaoui.tunidesk.exception.AccessDeniedException;
import com.saadaoui.tunidesk.exception.DuplicateResourceException;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import com.saadaoui.tunidesk.user.dto.CreateUserRequest;
import com.saadaoui.tunidesk.user.dto.ChangePasswordRequest;
import com.saadaoui.tunidesk.user.dto.ProfileUpdateRequest;
import com.saadaoui.tunidesk.user.dto.UpdateUserRequest;
import com.saadaoui.tunidesk.user.dto.UserOptionResponse;
import com.saadaoui.tunidesk.user.dto.UserResponse;
import com.saadaoui.tunidesk.user.mapper.UserMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_AGENT = "AGENT";

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;

    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(resolveRole(request.getRole())))
                .build();

        User saved = userRepository.save(user);

        log.info("User created: {} <{}> (id={})", saved.getFirstName(), saved.getEmail(), saved.getId());

        auditLogService.record(AuditConstants.ACTION_CREATE, AuditConstants.ENTITY_USER,
                saved.getId(), "Created user \"" + saved.getEmail() + "\"");

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException("Email already exists");
        }

        Set<Role> roles = request.getRoles().stream()
                .map(this::resolveRole)
                .collect(Collectors.toSet());

        ensureLastAdminSafety(user, request.getEnabled(), roles);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEnabled(request.getEnabled());
        user.setRoles(roles);

        User saved = userRepository.save(user);

        log.info("User updated: {} (id={})", saved.getEmail(), saved.getId());

        auditLogService.record(AuditConstants.ACTION_UPDATE, AuditConstants.ENTITY_USER,
                saved.getId(), "Updated user \"" + saved.getEmail() + "\"");

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {

        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {

        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, ProfileUpdateRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhoneNumber(request.getPhoneNumber());

        User saved = userRepository.save(user);

        auditLogService.record(AuditConstants.ACTION_UPDATE, AuditConstants.ENTITY_USER,
                saved.getId(), "Updated own profile \"" + saved.getEmail() + "\"");

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        auditLogService.record(AuditConstants.ACTION_UPDATE, AuditConstants.ENTITY_USER,
                user.getId(), "Changed password for \"" + user.getEmail() + "\"");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String search, Boolean enabled, Pageable pageable) {

        return userRepository
                .findAll(buildSpecification(search, enabled), pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOptionResponse> getRecipientOptions(String search) {

        String query = search == null ? "" : search.trim();

        return userRepository.searchEnabledUsers(query, PageRequest.of(0, 50))
                .stream()
                .map(this::toOption)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOptionResponse> getAgentOptions(String search) {

        String query = search == null ? "" : search.trim();

        return userRepository.searchEnabledByRole(ROLE_AGENT, query, PageRequest.of(0, 50))
                .stream()
                .map(this::toOption)
                .toList();
    }

    @Override
    @Transactional
    public void deleteUser(UUID id, String currentUserEmail) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getEmail().equals(currentUserEmail)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        ensureLastAdminSafety(user, false, user.getRoles());

        userRepository.deleteById(id);

        log.info("User deleted: {} (id={})", user.getEmail(), id);

        auditLogService.record(AuditConstants.ACTION_DELETE, AuditConstants.ENTITY_USER,
                id, "Deleted user \"" + user.getEmail() + "\"");
    }

    private Role resolveRole(String name) {

        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name));
    }

    private UserOptionResponse toOption(User user) {

        return new UserOptionResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }

    private void ensureLastAdminSafety(User target, Boolean newEnabled, Set<Role> newRoles) {

        boolean targetIsAdmin = target.getRoles().stream().anyMatch(role -> role.getName().equals(ROLE_ADMIN));
        boolean keepsAdmin = newRoles.stream().anyMatch(role -> role.getName().equals(ROLE_ADMIN));

        if (targetIsAdmin && (newEnabled == null || !newEnabled || !keepsAdmin)) {
            long admins = userRepository.countByEnabledTrueAndRolesName(ROLE_ADMIN);
            if (admins <= 1) {
                throw new AccessDeniedException(
                        "Cannot disable, delete or demote the last active ADMIN account");
            }
        }
    }

    private Specification<User> buildSpecification(String search, Boolean enabled) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
