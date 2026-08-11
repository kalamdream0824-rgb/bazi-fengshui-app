package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.config.BusinessException;
import com.bazi.app.domain.Order;
import com.bazi.app.domain.RedeemCode;
import com.bazi.app.domain.User;
import com.bazi.app.domain.constants.MemberPlans;
import com.bazi.app.dto.MembershipInfoDto;
import com.bazi.app.mapper.OrderMapper;
import com.bazi.app.mapper.RedeemCodeMapper;
import com.bazi.app.mapper.UserMapper;
import com.bazi.app.service.MembershipService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class MembershipServiceTest {

  @Autowired
  private MembershipService membershipService;

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private RedeemCodeMapper redeemCodeMapper;

  @Autowired
  private OrderMapper orderMapper;

  private Long createUser(String username) {
    User user = new User();
    user.setUsername(username);
    user.setPasswordHash("x");
    user.setCreatedAt(LocalDateTime.now());
    userMapper.insert(user);
    return user.getId();
  }

  private void seedCode(String code, String plan, int days) {
    RedeemCode redeemCode = new RedeemCode();
    redeemCode.setCode(code);
    redeemCode.setPlan(plan);
    redeemCode.setDurationDays(days);
    redeemCode.setCreatedAt(LocalDateTime.now());
    redeemCodeMapper.insert(redeemCode);
  }

  @Test
  @Transactional
  void redeemActivatesMembershipCaseInsensitive() {
    Long userId = createUser("mem1");
    seedCode("AAA111", "member_3m", 90);

    MembershipInfoDto info = membershipService.redeem(userId, "aaa111");
    assertTrue(info.isMember());
    assertEquals("member_3m", info.plan());
  }

  @Test
  @Transactional
  void redeemExtendsExistingExpiry() {
    Long userId = createUser("mem2");
    seedCode("AAA222", "member_1m", 30);
    seedCode("BBB222", "member_1m", 30);

    MembershipInfoDto first = membershipService.redeem(userId, "AAA222");
    MembershipInfoDto second = membershipService.redeem(userId, "BBB222");
    assertTrue(second.memberExpireAt().isAfter(first.memberExpireAt()));
  }

  @Test
  @Transactional
  void duplicateAndUnknownCodesRejected() {
    Long userId = createUser("mem3");
    seedCode("CCC333", "member_1m", 30);

    assertThrows(BusinessException.class, () -> membershipService.redeem(userId, "UNKNOWN"));
    membershipService.redeem(userId, "CCC333");
    assertThrows(BusinessException.class, () -> membershipService.redeem(userId, "CCC333"));
  }

  @Test
  @Transactional
  void payOrderActivatesMembershipAndIsIdempotent() {
    Long userId = createUser("pay1");
    MemberPlans.MemberPlan plan = MemberPlans.of(MemberPlans.MEMBER_1M);
    Order order = new Order();
    order.setUserId(userId);
    order.setPlan(plan.code());
    order.setAmountCents(plan.amountCents());
    order.setStatus("pending");
    order.setCreatedAt(LocalDateTime.now());
    orderMapper.insert(order);

    MembershipInfoDto first = membershipService.payOrder(userId, order.getId());
    MembershipInfoDto second = membershipService.payOrder(userId, order.getId());

    assertTrue(first.isMember());
    assertEquals("member_1m", first.plan());
    // 幂等：重复回调不重复顺延
    assertEquals(first.memberExpireAt(), second.memberExpireAt());
  }

  @Test
  @Transactional
  void payOrderRejectsNonOwner() {
    Long owner = createUser("pay-owner");
    Long other = createUser("pay-other");
    Order order = new Order();
    order.setUserId(owner);
    order.setPlan(MemberPlans.MEMBER_1M);
    order.setAmountCents(2990);
    order.setStatus("pending");
    order.setCreatedAt(LocalDateTime.now());
    orderMapper.insert(order);

    assertThrows(BusinessException.class, () -> membershipService.payOrder(other, order.getId()));
  }

  @Test
  @Transactional
  void grantMembershipIdempotentByProviderTradeNo() {
    Long userId = createUser("grant1");
    LocalDateTime now = LocalDateTime.now();

    MembershipInfoDto first = membershipService.grantMembership(
        userId, MemberPlans.MEMBER_1M, "mock", "TXN-1", 2990, now);
    MembershipInfoDto second = membershipService.grantMembership(
        userId, MemberPlans.MEMBER_1M, "mock", "TXN-1", 2990, now);

    assertTrue(first.isMember());
    assertEquals(first.memberExpireAt(), second.memberExpireAt());
    Long count = orderMapper.selectCount(new QueryWrapper<Order>()
        .eq("provider", "mock")
        .eq("provider_trade_no", "TXN-1")
        .eq("status", "paid"));
    assertEquals(1, count);
  }
}
