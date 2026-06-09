package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.entity.Notification;
import com.clas.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequireRole({"USER", "MERCHANT", "ADMIN"})
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/mine")
    public Result<List<Notification>> mine() {
        return Result.ok(notificationService.mine());
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteOne(@PathVariable Long id) {
        notificationService.deleteOne(id);
        return Result.ok();
    }

    @DeleteMapping("/all")
    public Result<Void> deleteAll() {
        notificationService.deleteAllMine();
        return Result.ok();
    }
}
