package com.saadaoui.tunidesk.user.controller;

import com.saadaoui.tunidesk.auth.service.UserService;
import com.saadaoui.tunidesk.user.dto.ChangePasswordRequest;
import com.saadaoui.tunidesk.user.dto.CreateUserRequest;
import com.saadaoui.tunidesk.user.dto.ProfileUpdateRequest;
import com.saadaoui.tunidesk.user.dto.UpdateUserRequest;
import com.saadaoui.tunidesk.user.dto.UserOptionResponse;
import com.saadaoui.tunidesk.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/me")
    public UserResponse updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        return userService.updateProfile(currentUser.getUsername(), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        userService.changePassword(currentUser.getUsername(), request);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        return userService.updateUser(id, request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {

        return userService.getUserById(id);
    }

    @GetMapping
    public Page<UserResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return userService.getAllUsers(search, enabled, createPageable(page, size, sortBy, direction));
    }

    @GetMapping("/options")
    public List<UserOptionResponse> getRecipientOptions(
            @RequestParam(required = false) String search) {

        return userService.getRecipientOptions(search);
    }

    @GetMapping("/agents")
    public List<UserOptionResponse> getAgentOptions(
            @RequestParam(required = false) String search) {

        return userService.getAgentOptions(search);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {

        userService.deleteUser(id, currentUser.getUsername());
    }

    private Pageable createPageable(int page, int size, String sortBy, String direction) {

        Sort.Direction sortDirection =
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

}
