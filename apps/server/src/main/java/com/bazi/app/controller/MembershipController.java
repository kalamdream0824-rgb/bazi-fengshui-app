package com.bazi.app.controller;

import com.bazi.app.dto.MembershipInfoDto;
import com.bazi.app.dto.RedeemRequest;
import com.bazi.app.service.MembershipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MembershipController {

  private final MembershipService membershipService;

  public MembershipController(MembershipService membershipService) {
    this.membershipService = membershipService;
  }

  @GetMapping("/me")
  public MembershipInfoDto me(HttpServletRequest request) {
    return membershipService.me(userId(request));
  }

  @PostMapping("/redeem")
  public MembershipInfoDto redeem(@Valid @RequestBody RedeemRequest redeemRequest, HttpServletRequest request) {
    return membershipService.redeem(userId(request), redeemRequest.code());
  }

  private static Long userId(HttpServletRequest request) {
    return (Long) request.getAttribute("userId");
  }
}
