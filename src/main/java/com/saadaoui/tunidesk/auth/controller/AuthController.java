package com.saadaoui.tunidesk.auth.controller;

import com.saadaoui.tunidesk.auth.dto.AuthResponse;
import com.saadaoui.tunidesk.auth.dto.LoginRequest;
import com.saadaoui.tunidesk.auth.dto.RegisterRequest;
import com.saadaoui.tunidesk.auth.service.AuthenticationService;
import com.saadaoui.tunidesk.auth.service.UserService;
import com.saadaoui.tunidesk.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    private final UserService userService;

    @PostMapping("/register")
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request) {

        return authenticationService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authenticationService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails currentUser) {

        return userService.getCurrentUser(currentUser.getUsername());
    }
}
