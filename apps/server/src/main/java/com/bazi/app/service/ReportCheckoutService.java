package com.bazi.app.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bazi.app.config.BusinessException;
import com.bazi.app.domain.BaziReport;
import com.bazi.app.domain.Order;
import com.bazi.app.domain.ReportOrderLink;
import com.bazi.app.domain.constants.ReportProducts;
import com.bazi.app.dto.ReportCheckoutDto;
import com.bazi.app.dto.ReportDto;
import com.bazi.app.dto.ReportPreviewRequest;
import com.bazi.app.mapper.BaziReportMapper;
import com.bazi.app.mapper.OrderMapper;
import com.bazi.app.mapper.ReportOrderLinkMapper;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportCheckoutService {

  private final ReportService reportService;
  private final OrderMapper orderMapper;
  private final ReportOrderLinkMapper linkMapper;
  private final BaziReportMapper reportMapper;
  private final boolean mockEnabled;

  public ReportCheckoutService(
      ReportService reportService,
      OrderMapper orderMapper,
      ReportOrderLinkMapper linkMapper,
      BaziReportMapper reportMapper,
      @Value("${app.pay.mock-enabled:true}") boolean mockEnabled) {
    this.reportService = reportService;
    this.orderMapper = orderMapper;
    this.linkMapper = linkMapper;
    this.reportMapper = reportMapper;
    this.mockEnabled = mockEnabled;
  }

  @Transactional
  public ReportCheckoutDto prepare(Long userId, ReportPreviewRequest request) throws Exception {
    var product = ReportProducts.of(request.topic(), request.edition());
    BaziReport report = reportService.createLocked(userId, request);
    LocalDateTime now = LocalDateTime.now();

    Order order = new Order();
    order.setUserId(userId);
    order.setPlan(product.code());
    order.setAmountCents(product.amountCents());
    order.setStatus("pending");
    order.setCreatedAt(now);
    orderMapper.insert(order);

    ReportOrderLink link = new ReportOrderLink();
    link.setOrderId(order.getId());
    link.setReportId(report.getId());
    link.setCreatedAt(now);
    linkMapper.insert(link);

    return new ReportCheckoutDto(
        order.getId(),
        report.getId(),
        product.topic(),
        product.edition(),
        product.amountCents(),
        order.getStatus());
  }

  @Transactional
  public ReportDto mockPayAndUnlock(Long userId, Long orderId) throws Exception {
    if (!mockEnabled) {
      throw new BusinessException("PAY_MOCK_DISABLED", "当前环境不支持模拟支付");
    }
    Order order = orderMapper.selectById(orderId);
    if (order == null || !userId.equals(order.getUserId())) {
      throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
    }
    ReportOrderLink link = linkMapper.selectOne(new QueryWrapper<ReportOrderLink>()
        .eq("order_id", orderId));
    if (link == null) {
      throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
    }

    if ("paid".equals(order.getStatus())) {
      return readyReport(userId, link.getReportId());
    }
    if (!"pending".equals(order.getStatus())) {
      throw new BusinessException("ORDER_STATUS_INVALID", "订单状态异常");
    }

    LocalDateTime now = LocalDateTime.now();
    int paidRows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
        .eq(Order::getId, orderId)
        .eq(Order::getUserId, userId)
        .eq(Order::getStatus, "pending")
        .set(Order::getStatus, "paid")
        .set(Order::getProvider, "mock")
        .set(Order::getProviderTradeNo, "MOCK-REPORT-" + orderId)
        .set(Order::getPaidAt, now));
    if (paidRows == 0) {
      Order after = orderMapper.selectById(orderId);
      if (after != null && "paid".equals(after.getStatus())) {
        return readyReport(userId, link.getReportId());
      }
      throw new BusinessException("ORDER_STATUS_INVALID", "订单状态异常");
    }

    int readyRows = reportMapper.update(null, new LambdaUpdateWrapper<BaziReport>()
        .eq(BaziReport::getId, link.getReportId())
        .eq(BaziReport::getUserId, userId)
        .eq(BaziReport::getStatus, "locked")
        .set(BaziReport::getStatus, "ready"));
    if (readyRows != 1) {
      throw new BusinessException("REPORT_UNLOCK_FAILED", "命书暂未解锁，本次支付未完成");
    }
    return readyReport(userId, link.getReportId());
  }

  private ReportDto readyReport(Long userId, Long reportId) throws Exception {
    return reportService.get(userId, reportId)
        .orElseThrow(() -> new BusinessException("REPORT_UNLOCK_FAILED", "命书暂未解锁，本次支付未完成"));
  }
}
