package com.bazi.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

  private final JwtUtil jwtUtil;

  public AuthInterceptor(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      throw new UnauthorizedException();
    }
    try {
      Long userId = jwtUtil.parse(header.substring(7));
      request.setAttribute("userId", userId);
      return true;
    } catch (Exception e) {
      throw new UnauthorizedException();
    }
  }
}
