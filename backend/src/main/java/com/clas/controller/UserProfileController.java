package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.AppealRequest;
import com.clas.dto.ProfileUpdateRequest;
import com.clas.entity.Appeal;
import com.clas.entity.User;
import com.clas.entity.UserPenalty;
import com.clas.service.AppealService;
import com.clas.service.PenaltyService;
import com.clas.service.UserAvatarUploadService;
import com.clas.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final UserAvatarUploadService userAvatarUploadService;
    private final PenaltyService penaltyService;
    private final AppealService appealService;

    public UserProfileController(
        UserProfileService userProfileService,
        UserAvatarUploadService userAvatarUploadService,
        PenaltyService penaltyService,
        AppealService appealService
    ) {
        this.userProfileService = userProfileService;
        this.userAvatarUploadService = userAvatarUploadService;
        this.penaltyService = penaltyService;
        this.appealService = appealService;
    }

    @GetMapping("/profile")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<User> profile() {
        return Result.ok(userProfileService.getProfile(UserContext.getUserId()));
    }

    @PutMapping("/profile")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<User> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok(userProfileService.updateProfile(UserContext.getUserId(), request));
    }

    @PostMapping("/profile/avatar")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<User> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(userAvatarUploadService.uploadAndUpdate(UserContext.getUserId(), file));
    }

    @GetMapping("/penalties/mine")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<List<UserPenalty>> myPenalties() {
        return Result.ok(penaltyService.listPenaltiesForUser(UserContext.getUserId()));
    }

    @PostMapping("/appeals")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<Appeal> submitAppeal(@Valid @RequestBody AppealRequest request) {
        return Result.ok(appealService.submit(UserContext.getUserId(), request));
    }

    @GetMapping("/appeals/mine")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<List<Appeal>> myAppeals() {
        return Result.ok(appealService.listMine(UserContext.getUserId()));
    }
}
