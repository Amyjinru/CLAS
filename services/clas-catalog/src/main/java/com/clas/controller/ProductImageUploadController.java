package com.clas.controller;

import com.clas.common.BusinessException;
import com.clas.common.LocalFileStorage;
import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.client.MerchantClient;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/product")
public class ProductImageUploadController {
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png");

    private final LocalFileStorage localFileStorage;
    private final MerchantClient merchantClient;

    public ProductImageUploadController(LocalFileStorage localFileStorage, MerchantClient merchantClient) {
        this.localFileStorage = localFileStorage;
        this.merchantClient = merchantClient;
    }

    @PostMapping("/upload-image")
    @RequireRole("MERCHANT")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        Long merchantId = merchantClient.getCurrentMerchantId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的商品图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("商品图片不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "product.jpg" : file.getOriginalFilename();
        String ext = original.contains(".")
            ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT)
            : ".jpg";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg/png 格式的商品图片");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
            throw new BusinessException("仅支持 jpg/png 格式的商品图片");
        }
        Path uploadDir = localFileStorage.resolveDirectory("product", String.valueOf(merchantId));
        return Result.ok(Map.of("url", localFileStorage.store(file, uploadDir, ext)));
    }
}
