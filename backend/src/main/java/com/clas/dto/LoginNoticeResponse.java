package com.clas.dto;

import java.time.LocalDateTime;

/** A pending request from another device; visible only to the currently active session. */
public record LoginNoticeResponse(String challengeId, LocalDateTime requestedAt) {
}
