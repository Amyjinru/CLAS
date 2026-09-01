#!/usr/bin/env python3
"""Fix clas-order migration: restore UTF-8 files and transform ReviewService."""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parent
BACKEND = ROOT.parent / "backend" / "src" / "main" / "java" / "com" / "clas"
ORDER = ROOT / "clas-order" / "src" / "main" / "java" / "com" / "clas"

COPY_FILES = [
    ("service/ContentModerationService.java", "service/ContentModerationService.java"),
    ("service/CouponService.java", "service/CouponService.java"),
    ("service/PaymentService.java", "service/PaymentService.java"),
    ("service/ReviewUploadService.java", "service/ReviewUploadService.java"),
]

def copy_utf8_files():
    for src_rel, dst_rel in COPY_FILES:
        src = BACKEND / src_rel
        dst = ORDER / dst_rel
        shutil.copy2(src, dst)
        print(f"copied {src_rel}")

def transform_review():
    src = BACKEND / "service" / "ReviewService.java"
    dst = ORDER / "service" / "ReviewService.java"
    text = src.read_text(encoding="utf-8")

    text = text.replace(
        "import com.clas.entity.Merchant;\n", ""
    ).replace(
        "import com.clas.entity.User;\n", ""
    ).replace(
        "import com.clas.mapper.MerchantMapper;\n",
        "import com.clas.client.CatalogClient;\nimport com.clas.client.IamClient;\nimport com.clas.dto.InternalUserSummary;\n"
    ).replace(
        "import com.clas.mapper.UserMapper;\n", ""
    ).replace(
        "import com.clas.service.NotificationService.NotificationTarget;\n", ""
    )

    text = text.replace(
        """    private final MerchantMapper merchantMapper;
    private final MerchantService merchantService;
    private final NotificationService notificationService;
    private final PenaltyService penaltyService;
    private final UserProfileService userProfileService;
    private final ContentModerationService contentModerationService;
    private final UserMapper userMapper;""",
        """    private final CatalogClient catalogClient;
    private final MerchantContextService merchantContextService;
    private final NotificationBridge notificationBridge;
    private final CommentPenaltyService commentPenaltyService;
    private final IamClient iamClient;
    private final ContentModerationService contentModerationService;"""
    )

    text = text.replace(
        """        MerchantMapper merchantMapper,
        MerchantService merchantService,
        NotificationService notificationService,
        PenaltyService penaltyService,
        UserProfileService userProfileService,
        ContentModerationService contentModerationService,
        UserMapper userMapper,""",
        """        CatalogClient catalogClient,
        MerchantContextService merchantContextService,
        NotificationBridge notificationBridge,
        CommentPenaltyService commentPenaltyService,
        IamClient iamClient,
        ContentModerationService contentModerationService,"""
    )

    text = text.replace(
        """        this.merchantMapper = merchantMapper;
        this.merchantService = merchantService;
        this.notificationService = notificationService;
        this.penaltyService = penaltyService;
        this.userProfileService = userProfileService;
        this.contentModerationService = contentModerationService;
        this.userMapper = userMapper;""",
        """        this.catalogClient = catalogClient;
        this.merchantContextService = merchantContextService;
        this.notificationBridge = notificationBridge;
        this.commentPenaltyService = commentPenaltyService;
        this.iamClient = iamClient;
        this.contentModerationService = contentModerationService;"""
    )

    text = text.replace("penaltyService.assertCanComment", "commentPenaltyService.assertCanComment")
    text = text.replace("merchantService.getCurrentMerchantId()", "merchantContextService.getCurrentMerchantId()")
    text = text.replace("notificationService.notifyAdmins", "notificationBridge.notifyAdmins")
    text = text.replace("notificationService.send(new NotificationTarget(", "notificationBridge.send(new NotificationBridge.NotificationTarget(")
    text = text.replace("merchantMapper.selectById(merchantId)", "catalogClient.getMerchant(merchantId)")
    text = text.replace(
        """        if (reviews.isEmpty()) {
            merchant.setScore(BigDecimal.ZERO);
            merchantMapper.updateById(merchant);
            return;
        }
        double average = reviews.stream().mapToInt(Review::getScore).average().orElse(0);
        merchant.setScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        merchantMapper.updateById(merchant);""",
        """        BigDecimal score = reviews.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(reviews.stream().mapToInt(Review::getScore).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);
        catalogClient.updateMerchantScore(merchantId, score);"""
    )

    text = text.replace(
        """        Map<String, User> users = userIds.isEmpty()
            ? Map.of()
            : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getPhone, user -> user));""",
        """        Map<String, InternalUserSummary> users = new java.util.HashMap<>();
        for (String userId : userIds) {
            InternalUserSummary summary = iamClient.getUser(userId);
            if (summary != null) {
                users.put(userId, summary);
            }
        }"""
    )

    text = text.replace("userProfileService.displayName(user)", "displayName(user)")
    text = text.replace("userProfileService.avatarOf(user)", "avatarOf(user)")

    text = text.replace(
        "        User user = context.users().get(review.getUserId());",
        "        InternalUserSummary user = context.users().get(review.getUserId());"
    )
    text = text.replace(
        "        User user = context.users().get(reply.getUserId());",
        "        InternalUserSummary user = context.users().get(reply.getUserId());"
    )

    text = text.replace(
        "        Map<String, User> users,",
        "        Map<String, InternalUserSummary> users,"
    )

    helper = """
    private String displayName(InternalUserSummary user) {
        if (user == null) {
            return "匿名用户";
        }
        if (user.username() != null && !user.username().isBlank()) {
            return user.username();
        }
        return user.userId();
    }

    private String avatarOf(InternalUserSummary user) {
        return null;
    }

"""
    text = text.replace(
        "    private void recalculateMerchantScore(Long merchantId) {",
        helper + "    private void recalculateMerchantScore(Long merchantId) {"
    )

    # getMerchantRating still references Merchant entity
    text = text.replace(
        """    public MerchantRatingResponse getMerchantRating(Long merchantId) {
        Merchant merchant = catalogClient.getMerchant(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        List<Review> reviews = listRawByMerchantId(merchantId);
        return new MerchantRatingResponse(merchantId, merchant.getScore(), (long) reviews.size());
    }""",
        """    public MerchantRatingResponse getMerchantRating(Long merchantId) {
        var merchant = catalogClient.getMerchant(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        List<Review> reviews = listRawByMerchantId(merchantId);
        return new MerchantRatingResponse(merchantId, merchant.getScore(), (long) reviews.size());
    }"""
    )

    text = text.replace(
        """        List<Review> reviews = listRawByMerchantId(merchantId);
        Merchant merchant = catalogClient.getMerchant(merchantId);
        if (merchant == null) {
            return;
        }
        BigDecimal score = reviews.isEmpty()""",
        """        List<Review> reviews = listRawByMerchantId(merchantId);
        if (catalogClient.getMerchant(merchantId) == null) {
            return;
        }
        BigDecimal score = reviews.isEmpty()"""
    )

    dst.write_text(text, encoding="utf-8")
    print("transformed ReviewService.java")

if __name__ == "__main__":
    copy_utf8_files()
    transform_review()
