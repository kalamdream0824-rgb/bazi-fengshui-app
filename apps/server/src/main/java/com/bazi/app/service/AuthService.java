package com.bazi.app.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.config.JwtUtil;
import com.bazi.app.config.LoginRateLimiter;
import com.bazi.app.config.BusinessException;
import com.bazi.app.domain.User;
import com.bazi.app.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserMapper userMapper;
  private final JwtUtil jwtUtil;
  private final LoginRateLimiter rateLimiter;
  private final PasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthService(UserMapper userMapper, JwtUtil jwtUtil, LoginRateLimiter rateLimiter) {
    this.userMapper = userMapper;
    this.jwtUtil = jwtUtil;
    this.rateLimiter = rateLimiter;
  }

  public String register(String username, String password) {
    if (username == null || username.isBlank() || password == null || password.length() < 6) {
      throw new BusinessException("USERNAME_INVALID", "用户名不能为空，密码至少 6 位");
    }
    Long count = userMapper.selectCount(new QueryWrapper<User>().eq("username", username));
    if (count > 0) {
      throw new BusinessException("USERNAME_EXISTS", "用户名已存在");
    }
    User user = new User();
    user.setUsername(username.trim());
    user.setPasswordHash(encoder.encode(password));
    user.setCreatedAt(LocalDateTime.now());
    userMapper.insert(user);
    return jwtUtil.create(user.getId());
  }

  public String login(String username, String password) {
    rateLimiter.check(username);
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    if (user == null || !encoder.matches(password, user.getPasswordHash())) {
      rateLimiter.recordFailure(username);
      throw new BusinessException("BAD_CREDENTIALS", "用户名或密码错误");
    }
    rateLimiter.clear(username);
    return jwtUtil.create(user.getId());
  }
}
