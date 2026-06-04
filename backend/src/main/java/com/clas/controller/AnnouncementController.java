package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.AnnouncementRequest;
import com.clas.entity.Announcement;
import com.clas.service.AnnouncementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/list")
    public Result<List<Announcement>> list() {
        return Result.ok(announcementService.listPublished());
    }

    @RequireRole("ADMIN")
    @PostMapping("/create")
    public Result<Announcement> create(@Valid @RequestBody AnnouncementRequest request) {
        return Result.ok(announcementService.create(request));
    }

    @RequireRole("ADMIN")
    @PutMapping("/{id}")
    public Result<Announcement> update(
        @PathVariable Long id,
        @Valid @RequestBody AnnouncementRequest request
    ) {
        return Result.ok(announcementService.update(id, request));
    }

    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.ok();
    }
}
