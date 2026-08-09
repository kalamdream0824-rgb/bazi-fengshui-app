package com.bazi.app.config;

public class UnauthorizedException extends RuntimeException {

  public UnauthorizedException() {
    super("未登录或登录已过期");
  }
}
