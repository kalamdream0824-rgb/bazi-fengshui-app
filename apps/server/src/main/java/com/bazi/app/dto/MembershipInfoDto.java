package com.bazi.app.dto;

import java.time.LocalDateTime;

public record MembershipInfoDto(
    String username,
    String plan,
    LocalDateTime memberExpireAt,
    boolean isMember) {
}
