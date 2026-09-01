package com.clas.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class AnnouncementRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private Boolean pinned;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getPinned() { return pinned; }
    public void setPinned(Boolean pinned) { this.pinned = pinned; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
}
