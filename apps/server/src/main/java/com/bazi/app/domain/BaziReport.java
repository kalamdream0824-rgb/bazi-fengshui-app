package com.bazi.app.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("bazi_report")
public class BaziReport {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private String subject;
  private String topic;
  private String edition;
  private String status;
  private String contentVersion;
  private String requestJson;
  private String contextJson;
  private String contentJson;
  private LocalDateTime createdAt;
  private LocalDateTime generatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }
  public String getTopic() { return topic; }
  public void setTopic(String topic) { this.topic = topic; }
  public String getEdition() { return edition; }
  public void setEdition(String edition) { this.edition = edition; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getContentVersion() { return contentVersion; }
  public void setContentVersion(String contentVersion) { this.contentVersion = contentVersion; }
  public String getRequestJson() { return requestJson; }
  public void setRequestJson(String requestJson) { this.requestJson = requestJson; }
  public String getContextJson() { return contextJson; }
  public void setContextJson(String contextJson) { this.contextJson = contextJson; }
  public String getContentJson() { return contentJson; }
  public void setContentJson(String contentJson) { this.contentJson = contentJson; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getGeneratedAt() { return generatedAt; }
  public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
