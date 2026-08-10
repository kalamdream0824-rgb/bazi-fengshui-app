package com.bazi.app.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("bazi_order")
public class Order {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private String plan;
  private Integer amountCents;
  private String status;
  private String provider;
  private String providerTradeNo;
  private LocalDateTime createdAt;
  private LocalDateTime paidAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getPlan() {
    return plan;
  }

  public void setPlan(String plan) {
    this.plan = plan;
  }

  public Integer getAmountCents() {
    return amountCents;
  }

  public void setAmountCents(Integer amountCents) {
    this.amountCents = amountCents;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getProviderTradeNo() {
    return providerTradeNo;
  }

  public void setProviderTradeNo(String providerTradeNo) {
    this.providerTradeNo = providerTradeNo;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(LocalDateTime paidAt) {
    this.paidAt = paidAt;
  }
}
