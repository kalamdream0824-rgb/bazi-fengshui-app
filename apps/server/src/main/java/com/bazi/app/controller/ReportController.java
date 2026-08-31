package com.bazi.app.controller;

import com.bazi.app.dto.ReportCheckoutDto;
import com.bazi.app.dto.ReportDto;
import com.bazi.app.dto.ReportPreviewRequest;
import com.bazi.app.service.ReportCheckoutService;
import com.bazi.app.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

  private final ReportService reportService;
  private final ReportCheckoutService checkoutService;

  public ReportController(ReportService reportService, ReportCheckoutService checkoutService) {
    this.reportService = reportService;
    this.checkoutService = checkoutService;
  }

  @PostMapping
  public ReportDto create(
      @Valid @RequestBody ReportPreviewRequest request,
      HttpServletRequest httpRequest) throws Exception {
    return reportService.create(userId(httpRequest), request);
  }

  @PostMapping("/checkout")
  public ReportCheckoutDto checkout(
      @Valid @RequestBody ReportPreviewRequest request,
      HttpServletRequest httpRequest) throws Exception {
    return checkoutService.prepare(userId(httpRequest), request);
  }

  @PostMapping("/checkout/{orderId}/mock-pay")
  public ReportDto mockPayCheckout(
      @PathVariable Long orderId,
      HttpServletRequest httpRequest) throws Exception {
    return checkoutService.mockPayAndUnlock(userId(httpRequest), orderId);
  }

  @GetMapping
  public List<ReportDto> list(HttpServletRequest httpRequest) throws Exception {
    return reportService.list(userId(httpRequest));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ReportDto> get(
      @PathVariable Long id,
      HttpServletRequest httpRequest) throws Exception {
    return reportService.get(userId(httpRequest), id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private static Long userId(HttpServletRequest request) {
    return (Long) request.getAttribute("userId");
  }
}
