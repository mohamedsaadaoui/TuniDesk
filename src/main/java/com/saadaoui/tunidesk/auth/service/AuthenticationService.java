package com.saadaoui.tunidesk.auth.service;

import com.saadaoui.tunidesk.auth.dto.AuthResponse;
import com.saadaoui.tunidesk.auth.dto.ForgotPasswordRequest;
import com.saadaoui.tunidesk.auth.dto.LoginRequest;
import com.saadaoui.tunidesk.auth.dto.RegisterRequest;
import com.saadaoui.tunidesk.auth.dto.ResetPasswordRequest;

public interface AuthenticationService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
