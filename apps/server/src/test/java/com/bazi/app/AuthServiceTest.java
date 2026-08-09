package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class AuthServiceTest {

  @Autowired
  private AuthService authService;

  @Test
  @Transactional
  void registerAndLoginRoundtrip() {
    String token = authService.register("tester", "pass123");
    assertNotNull(token);
    assertNotNull(authService.login("tester", "pass123"));
  }

  @Test
  @Transactional
  void duplicateUsernameRejected() {
    authService.register("dup", "pass123");
    assertThrows(IllegalArgumentException.class, () -> authService.register("dup", "pass123"));
  }

  @Test
  @Transactional
  void wrongPasswordRejected() {
    authService.register("u2", "pass123");
    assertThrows(IllegalArgumentException.class, () -> authService.login("u2", "wrong"));
  }
}
