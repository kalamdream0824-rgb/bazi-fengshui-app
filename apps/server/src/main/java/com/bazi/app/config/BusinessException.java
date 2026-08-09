package com.bazi.app.config;

/** 业务异常：携带错误码，由全局处理器统一映射为 4xx 响应 */
public class BusinessException extends RuntimeException {

  private final String code;

  public BusinessException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
