package com.bazi.app.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.config.BusinessException;
import com.bazi.app.config.UnauthorizedException;
import com.bazi.app.domain.BaziReport;
import com.bazi.app.domain.User;
import com.bazi.app.mapper.BaziReportMapper;
import com.bazi.app.mapper.UserMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/** Serializes one user's report consumption and counts only successfully saved reports. */
@Service
public class ReportEntitlementService {
  static final int MEMBER_DAILY_LIMIT = 3;

  private final UserMapper userMapper;
  private final BaziReportMapper reportMapper;

  public ReportEntitlementService(UserMapper userMapper, BaziReportMapper reportMapper) {
    this.userMapper = userMapper;
    this.reportMapper = reportMapper;
  }

  /** Rejects the member-only direct route before expensive report generation starts. */
  public void requireDirectGenerationAccess(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) throw new UnauthorizedException();
    if (!isActiveMember(user, LocalDateTime.now())) {
      throw new BusinessException("REPORT_PAYMENT_REQUIRED", "请先购买这份命书，本次未扣费");
    }
  }

  /** Must run inside the same transaction as the subsequent report insert. */
  public void requireSuccessfulReportSlot(Long userId) {
    User user = userMapper.selectByIdForUpdate(userId);
    if (user == null) throw new UnauthorizedException();

    LocalDateTime now = LocalDateTime.now();
    if (!isActiveMember(user, now)) {
      throw new BusinessException("REPORT_PAYMENT_REQUIRED", "请先购买这份命书，本次未扣费");
    }

    LocalDate today = now.toLocalDate();
    Long successfulToday = reportMapper.selectCount(new QueryWrapper<BaziReport>()
        .eq("user_id", userId)
        .eq("status", "ready")
        .notInSql("id", "SELECT report_id FROM bazi_report_order_link")
        .ge("generated_at", today.atStartOfDay())
        .lt("generated_at", today.plusDays(1).atStartOfDay()));
    if (successfulToday >= MEMBER_DAILY_LIMIT) {
      throw new BusinessException("MEMBER_DAILY_REPORT_LIMIT",
          "今天的3份会员命书已全部生成；继续生成需单独购买，本次未扣费");
    }
  }

  private boolean isActiveMember(User user, LocalDateTime now) {
    return user.getMemberExpireAt() != null && user.getMemberExpireAt().isAfter(now);
  }
}
