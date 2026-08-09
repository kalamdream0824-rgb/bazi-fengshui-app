package com.bazi.app.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 登录失败限流（内存实现，单实例部署够用；多实例需换 Redis 等共享存储） */
@Component
public class LoginRateLimiter {

  private final int maxAttempts;
  private final long lockMillis;
  private final Map<String, Entry> attempts = new ConcurrentHashMap<>();

  public LoginRateLimiter(
      @Value("${app.security.login.max-attempts:5}") int maxAttempts,
      @Value("${app.security.login.lock-minutes:5}") long lockMinutes) {
    this.maxAttempts = maxAttempts;
    this.lockMillis = lockMinutes * 60_000L;
  }

  public void check(String key) {
    Entry entry = attempts.get(key);
    if (entry == null) {
      return;
    }
    long now = System.currentTimeMillis();
    if (entry.failures >= maxAttempts) {
      if (now < entry.lockedUntil) {
      long minutes = Math.max(1, (entry.lockedUntil - now) / 60_000 + 1);
      throw new TooManyRequestsException("尝试次数过多，请 " + minutes + " 分钟后再试");
      }
      attempts.remove(key);
    }
  }

  public void recordFailure(String key) {
    long now = System.currentTimeMillis();
    attempts.compute(key, (k, entry) -> {
      Entry current = (entry == null || (entry.failures >= maxAttempts && now >= entry.lockedUntil)) ? new Entry() : entry;
      current.failures += 1;
      if (current.failures >= maxAttempts) {
        current.lockedUntil = now + lockMillis;
      }
      return current;
    });
  }

  public void clear(String key) {
    attempts.remove(key);
  }

  private static class Entry {
    private int failures;
    private long lockedUntil;
  }
}
