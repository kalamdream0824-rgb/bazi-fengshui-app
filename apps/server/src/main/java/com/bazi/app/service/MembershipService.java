package com.bazi.app.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bazi.app.config.BusinessException;
import com.bazi.app.config.UnauthorizedException;
import com.bazi.app.domain.constants.MemberPlans;
import com.bazi.app.domain.constants.MemberPlans.MemberPlan;
import com.bazi.app.domain.Order;
import com.bazi.app.domain.RedeemCode;
import com.bazi.app.domain.User;
import com.bazi.app.dto.MembershipInfoDto;
import com.bazi.app.mapper.OrderMapper;
import com.bazi.app.mapper.RedeemCodeMapper;
import com.bazi.app.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

  private final UserMapper userMapper;
  private final RedeemCodeMapper redeemCodeMapper;
  private final OrderMapper orderMapper;

  public MembershipService(UserMapper userMapper, RedeemCodeMapper redeemCodeMapper, OrderMapper orderMapper) {
    this.userMapper = userMapper;
    this.redeemCodeMapper = redeemCodeMapper;
    this.orderMapper = orderMapper;
  }

  public MembershipInfoDto me(Long userId) {
    return toDto(requireUser(userId));
  }

  @Transactional
  public MembershipInfoDto redeem(Long userId, String rawCode) {
    if (rawCode == null || rawCode.isBlank()) {
      throw new BusinessException("CODE_INVALID", "请输入兑换码");
    }
    String code = rawCode.trim().toUpperCase();
    RedeemCode redeemCode = redeemCodeMapper.selectOne(new QueryWrapper<RedeemCode>().eq("code", code));
    if (redeemCode == null) {
      throw new BusinessException("CODE_NOT_FOUND", "兑换码无效");
    }
    if (redeemCode.getUsedBy() != null) {
      throw new BusinessException("CODE_USED", "兑换码已被使用");
    }

    LocalDateTime now = LocalDateTime.now();
    MembershipInfoDto info = grantMembership(
        userId, redeemCode.getPlan(), "redeem", redeemCode.getCode(), 0, now);

    redeemCode.setUsedBy(userId);
    redeemCode.setUsedAt(now);
    redeemCodeMapper.updateById(redeemCode);

    return info;
  }

  /**
   * 按一笔已支付渠道交易开通/顺延会员（幂等：同一 provider + provider_trade_no 已落账则不重复顺延）。
   * 兑换码（provider=redeem）与未来真实渠道共用。
   */
  @Transactional
  public MembershipInfoDto grantMembership(
      Long userId, String plan, String provider, String providerTradeNo, int amountCents, LocalDateTime at) {
    Long alreadyPaid = orderMapper.selectCount(new QueryWrapper<Order>()
        .eq("provider", provider)
        .eq("provider_trade_no", providerTradeNo)
        .eq("status", "paid"));
    if (alreadyPaid > 0) {
      return toDto(requireUser(userId));
    }

    extendMembership(userId, plan);

    Order order = new Order();
    order.setUserId(userId);
    order.setPlan(plan);
    order.setAmountCents(amountCents);
    order.setStatus("paid");
    order.setProvider(provider);
    order.setProviderTradeNo(providerTradeNo);
    order.setCreatedAt(at);
    order.setPaidAt(at);
    orderMapper.insert(order);

    return toDto(requireUser(userId));
  }

  /** 模拟支付回调落账：CAS 将 pending 置为 paid（幂等，重复回调不重复顺延），随后开通会员。 */
  @Transactional
  public MembershipInfoDto payOrder(Long userId, Long orderId) {
    User user = requireUser(userId);
    Order order = orderMapper.selectById(orderId);
    if (order == null || !userId.equals(order.getUserId())) {
      throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
    }

    LocalDateTime now = LocalDateTime.now();
    int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
        .eq(Order::getId, orderId)
        .eq(Order::getStatus, "pending")
        .set(Order::getStatus, "paid")
        .set(Order::getProvider, "mock")
        .set(Order::getProviderTradeNo, "MOCK-" + orderId)
        .set(Order::getPaidAt, now));
    if (rows == 0) {
      Order after = orderMapper.selectById(orderId);
      if (after != null && "paid".equals(after.getStatus())) {
        return toDto(user); // 幂等：已支付不重复顺延
      }
      throw new BusinessException("ORDER_STATUS_INVALID", "订单状态异常");
    }

    extendMembership(userId, order.getPlan());
    return toDto(requireUser(userId));
  }

  private void extendMembership(Long userId, String plan) {
    MemberPlan memberPlan = MemberPlans.of(plan);
    User user = requireUser(userId);
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime base = user.getMemberExpireAt() != null && user.getMemberExpireAt().isAfter(now)
        ? user.getMemberExpireAt()
        : now;
    user.setPlan(plan);
    user.setMemberExpireAt(base.plusDays(memberPlan.days()));
    userMapper.updateById(user);
  }

  private User requireUser(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new UnauthorizedException();
    }
    return user;
  }

  private MembershipInfoDto toDto(User user) {
    return new MembershipInfoDto(
        user.getUsername(),
        user.getPlan(),
        user.getMemberExpireAt(),
        user.getMemberExpireAt() != null && user.getMemberExpireAt().isAfter(LocalDateTime.now()));
  }
}
