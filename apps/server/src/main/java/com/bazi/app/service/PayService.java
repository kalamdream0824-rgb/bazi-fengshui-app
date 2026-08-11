package com.bazi.app.service;

import com.bazi.app.config.BusinessException;
import com.bazi.app.domain.Order;
import com.bazi.app.domain.constants.MemberPlans;
import com.bazi.app.domain.constants.MemberPlans.MemberPlan;
import com.bazi.app.dto.MembershipInfoDto;
import com.bazi.app.dto.OrderDto;
import com.bazi.app.mapper.OrderMapper;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayService {

  private final OrderMapper orderMapper;
  private final MembershipService membershipService;
  private final boolean mockEnabled;

  public PayService(
      OrderMapper orderMapper,
      MembershipService membershipService,
      @Value("${app.pay.mock-enabled:true}") boolean mockEnabled) {
    this.orderMapper = orderMapper;
    this.membershipService = membershipService;
    this.mockEnabled = mockEnabled;
  }

  @Transactional
  public OrderDto createOrder(Long userId, String planCode) {
    MemberPlan plan = MemberPlans.of(planCode);
    LocalDateTime now = LocalDateTime.now();
    Order order = new Order();
    order.setUserId(userId);
    order.setPlan(plan.code());
    order.setAmountCents(plan.amountCents());
    order.setStatus("pending");
    order.setCreatedAt(now);
    orderMapper.insert(order);
    return OrderDto.from(order);
  }

  public MembershipInfoDto mockPay(Long userId, Long orderId) {
    if (!mockEnabled) {
      throw new BusinessException("PAY_MOCK_DISABLED", "当前环境不支持模拟支付");
    }
    return membershipService.payOrder(userId, orderId);
  }
}
