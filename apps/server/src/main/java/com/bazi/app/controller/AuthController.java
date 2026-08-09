package com.bazi.app.controller;

import com.bazi.app.dto.AuthRequest;
import com.bazi.app.dto.AuthResponse;
import com.bazi.app.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody AuthRequest request) {
    return new AuthResponse(authService.register(request.username(), request.password()), request.username());
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody AuthRequest request) {
    return new AuthResponse(authService.login(request.username(), request.password()), request.username());
  }
}
