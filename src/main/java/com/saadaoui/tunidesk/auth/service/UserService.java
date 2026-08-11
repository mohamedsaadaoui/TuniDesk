package com.saadaoui.tunidesk.auth.service;

import com.saadaoui.tunidesk.user.dto.CreateUserRequest;
import com.saadaoui.tunidesk.user.dto.UpdateUserRequest;
import com.saadaoui.tunidesk.user.dto.UserOptionResponse;
import com.saadaoui.tunidesk.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    UserResponse getUserById(UUID id);

    Page<UserResponse> getAllUsers(String search, Boolean enabled, Pageable pageable);

    List<UserOptionResponse> getRecipientOptions(String search);

    List<UserOptionResponse> getAgentOptions(String search);

    void deleteUser(UUID id, String currentUserEmail);

}
