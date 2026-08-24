package com.marketplace.user.auth.controller;

import com.marketplace.user.auth.api.AuthApi;
import com.marketplace.user.auth.dto.AuthResponse;
import com.marketplace.user.auth.dto.LoginRequest;
import com.marketplace.user.auth.dto.RefreshRequest;
import com.marketplace.user.auth.dto.RegisterRequest;
import com.marketplace.user.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<AuthResponse> register(RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Override
    public ResponseEntity<AuthResponse> refresh(RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}