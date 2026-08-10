package com.bazi.app.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.config.BusinessException;
import com.bazi.app.domain.Order;
import com.bazi.app.domain.RedeemCode;
import com.bazi.app.domain.User;
import com.bazi.app.dto.MembershipInfoDto;
import com.bazi.app.mapper.OrderMapper;
import com.bazi.app.mapper.RedeemCodeMapper;
import com.bazi.app.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

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
    User user = userMapper.selectById(userId);
    return toDto(user);
  }

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

    User user = userMapper.selectById(userId);
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime base = user.getMemberExpireAt() != null && user.getMemberExpireAt().isAfter(now)
        ? user.getMemberExpireAt()
        : now;
    LocalDateTime expire = base.plusDays(redeemCode.getDurationDays());

    user.setPlan(redeemCode.getPlan());
    user.setMemberExpireAt(expire);
    userMapper.updateById(user);

    redeemCode.setUsedBy(userId);
    redeemCode.setUsedAt(now);
    redeemCodeMapper.updateById(redeemCode);

    Order order = new Order();
    order.setUserId(userId);
    order.setPlan(redeemCode.getPlan());
    order.setAmountCents(0);
    order.setStatus("paid");
    order.setProvider("redeem");
    order.setProviderTradeNo(redeemCode.getCode());
    order.setCreatedAt(now);
    order.setPaidAt(now);
    orderMapper.insert(order);

    return toDto(user);
  }

  private MembershipInfoDto toDto(User user) {
    return new MembershipInfoDto(
        user.getUsername(),
        user.getPlan(),
        user.getMemberExpireAt(),
        user.getMemberExpireAt() != null && user.getMemberExpireAt().isAfter(LocalDateTime.now()));
  }
}
