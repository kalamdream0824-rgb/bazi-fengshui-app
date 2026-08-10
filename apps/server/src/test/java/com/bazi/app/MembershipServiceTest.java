package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.config.BusinessException;
import com.bazi.app.domain.RedeemCode;
import com.bazi.app.domain.User;
import com.bazi.app.dto.MembershipInfoDto;
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
}
