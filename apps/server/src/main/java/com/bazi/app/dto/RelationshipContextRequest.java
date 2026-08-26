package com.bazi.app.dto;

import com.bazi.app.report.relationship.RelationshipStatus;
import jakarta.validation.constraints.NotBlank;

public record RelationshipContextRequest(
    @NotBlank String status) {

  public RelationshipStatus toDomain() {
    return RelationshipStatus.fromCode(status);
  }
}
