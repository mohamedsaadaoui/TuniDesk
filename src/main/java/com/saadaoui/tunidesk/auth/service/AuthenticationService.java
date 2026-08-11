package com.saadaoui.tunidesk.auth.service;

import com.saadaoui.tunidesk.auth.dto.AuthResponse;
import com.saadaoui.tunidesk.auth.dto.LoginRequest;
import com.saadaoui.tunidesk.auth.dto.RegisterRequest;

public interface AuthenticationService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

}
