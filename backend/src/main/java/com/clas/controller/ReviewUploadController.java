package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.service.ReviewUploadService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/review/upload")
public class ReviewUploadController {
    private final ReviewUploadService reviewUploadService;

    public ReviewUploadController(ReviewUploadService reviewUploadService) {
        this.reviewUploadService = reviewUploadService;
    }

    @PostMapping
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<List<String>> upload(@RequestParam("files") MultipartFile[] files) {
        return Result.ok(reviewUploadService.upload(files, UserContext.getUserId()));
    }
}
