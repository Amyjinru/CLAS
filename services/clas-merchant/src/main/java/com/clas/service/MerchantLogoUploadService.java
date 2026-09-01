package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.common.LocalFileStorage;
import com.clas.dto.MerchantResponse;
import com.clas.entity.Merchant;
import com.clas.mapper.MerchantMapper;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MerchantLogoUploadService {
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png");

    private final LocalFileStorage localFileStorage;
    private final MerchantMapper merchantMapper;
    private final MerchantService merchantService;
    private final ContentModerationService contentModerationService;

    public MerchantLogoUploadService(
        LocalFileStorage localFileStorage,
        MerchantMapper merchantMapper,
        MerchantService merchantService,
        ContentModerationService contentModerationService
    ) {
        this.localFileStorage = localFileStorage;
        this.merchantMapper = merchantMapper;
        this.merchantService = merchantService;
        this.contentModerationService = contentModerationService;
    }

    @Transactional
    public MerchantResponse uploadAndUpdate(MultipartFile file) {
        Long merchantId = merchantService.getCurrentMerchantId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的店铺头像");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("店铺头像不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "logo.jpg" : file.getOriginalFilename();
        String ext = original.contains(".")
            ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT)
            : ".jpg";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg/png 格式的店铺头像");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
            throw new BusinessException("仅支持 jpg/png 格式的店铺头像");
        }
        contentModerationService.assertAvatarAllowed(file);

        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("当前用户未入驻为商家");
        }
        Path uploadDir = localFileStorage.resolveDirectory("merchant-logo", String.valueOf(merchantId));
        merchant.setLogo(localFileStorage.store(file, uploadDir, ext));
        merchantMapper.updateById(merchant);
        return merchantService.getMerchantByUserId(merchant.getUserId());
    }
}
