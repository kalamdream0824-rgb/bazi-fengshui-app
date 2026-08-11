package com.bazi.app.controller;

import com.bazi.app.config.BusinessException;
import com.bazi.app.dto.CreateOrderRequest;
import com.bazi.app.dto.MembershipInfoDto;
import com.bazi.app.dto.OrderDto;
import com.bazi.app.dto.PayCallbackRequest;
import com.bazi.app.service.PayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PayController {

  private final PayService payService;

  public PayController(PayService payService) {
    this.payService = payService;
  }

  @PostMapping("/orders")
  public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest request, HttpServletRequest http) {
    return payService.createOrder(userId(http), request.plan());
  }

  @PostMapping("/pay/mock-success/{orderId}")
  public MembershipInfoDto mockSuccess(@PathVariable Long orderId, HttpServletRequest http) {
    return payService.mockPay(userId(http), orderId);
  }

  /** 真实支付渠道回调占位：资质接入后启用签名校验并落账。 */
  @PostMapping("/pay/callback")
  public Map<String, String> callback(@Valid @RequestBody PayCallbackRequest request) {
    throw new BusinessException("PAY_CALLBACK_NOT_READY", "真实支付回调待接入渠道后启用");
  }

  private static Long userId(HttpServletRequest request) {
    return (Long) request.getAttribute("userId");
  }
}
