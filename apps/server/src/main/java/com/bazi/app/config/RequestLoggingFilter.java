package com.bazi.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 统一访问日志：方法 / URI / 状态 / 耗时 / 用户；慢请求（>500ms）单独 WARN */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
  private static final long SLOW_MS = 500;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long start = System.currentTimeMillis();
    try {
      chain.doFilter(request, response);
    } finally {
      long elapsed = System.currentTimeMillis() - start;
      Object userId = request.getAttribute("userId");
      String line = String.format("%s %s -> %d (%dms) user=%s",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          elapsed,
          userId == null ? "-" : userId);
      if (elapsed >= SLOW_MS) {
        log.warn("SLOW " + line);
      } else {
        log.info(line);
      }
    }
  }
}
