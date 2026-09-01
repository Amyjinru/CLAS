#!/usr/bin/env python3
"""Bulk copy compat-domain files from backend to clas-compat."""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parent
BACKEND = ROOT.parent / "backend" / "src" / "main" / "java" / "com" / "clas"
COMPAT = ROOT / "clas-compat" / "src" / "main" / "java" / "com" / "clas"
ORDER = ROOT / "clas-order" / "src" / "main" / "java" / "com" / "clas"

ENTITY_PREFIXES = (
    "Announcement", "Chat", "DeliveryCall", "DeliveryException", "Rider",
)
ENTITY_EXACT = {"Orders", "OrderItem", "Merchant", "Product", "User", "UserRole", "UserPenalty", "OrderLifecycleEvent"}

MAPPER_PREFIXES = (
    "Announcement", "Chat", "DeliveryCall", "DeliveryException", "Rider",
)
MAPPER_EXACT = {
    "OrdersMapper", "OrderItemMapper", "MerchantMapper", "ProductMapper",
    "UserMapper", "UserRoleMapper", "UserPenaltyMapper", "OrderLifecycleEventMapper",
}

SERVICE_NAMES = {
    "RiderService", "RiderApplicationService", "RiderLocationService", "RiderDispatchService",
    "RiderDeliveryService", "RiderContactService", "RiderMerchantContactService",
    "RiderWithdrawalService", "RiderReviewService", "RiderInfoService", "RiderSettlementService",
    "RiderIdentityCrypto", "DeliveryTrackingService", "ChatService", "AnnouncementService",
    "StatisticsService", "ContentModerationService", "AmapRouteService",
    "SessionTouchService", "AuthPenaltyService", "CommentPenaltyService",
}

CONTROLLERS = {
    "RiderController", "RiderApplicationController", "DeliveryTrackingController",
    "RiderMerchantContactController", "ChatController", "AdminController",
    "AnnouncementController", "PublicStatsController",
}

DTO_PREFIXES = (
    "Rider", "Chat", "Delivery", "Conversation", "Announcement",
    "Dashboard", "OrderStats", "SalesOverview", "MerchantRanking", "TopProduct",
    "RefundDispute", "Penalty", "Appeal", "RoleApplicationRecord",
)
DTO_EXACT = {
    "InternalNotificationRequest", "InternalUserSummary", "InternalAddressResponse",
    "OrderResponse", "OrderLifecycleEventResponse", "MerchantStatsDTO",
}

CONFIG_FROM_ORDER = [
    "config/AuthInterceptor.java",
    "config/RequireRole.java",
    "config/UserContext.java",
    "config/OrderWebConfig.java",
]

def should_copy(name: str, prefixes, exact=()) -> bool:
    if name.replace(".java", "") in exact:
        return True
    base = name.replace(".java", "")
    return any(base.startswith(p) for p in prefixes)


def copy_dir(src_dir: Path, dst_dir: Path, filter_fn):
    dst_dir.mkdir(parents=True, exist_ok=True)
    for src in src_dir.glob("*.java"):
        if filter_fn(src.name):
            shutil.copy2(src, dst_dir / src.name)
            print(f"  {src_dir.name}/{src.name}")


def copy_repository():
    for sub in ["repository", "repository/impl"]:
        src = BACKEND / sub
        if not src.exists():
            continue
        dst = COMPAT / sub
        dst.mkdir(parents=True, exist_ok=True)
        for f in src.glob("Announcement*.java"):
            shutil.copy2(f, dst / f.name)
            print(f"  {sub}/{f.name}")


def main():
    print("entities")
    copy_dir(BACKEND / "entity", COMPAT / "entity", lambda n: should_copy(n, ENTITY_PREFIXES, ENTITY_EXACT))
    print("mappers")
    copy_dir(BACKEND / "mapper", COMPAT / "mapper", lambda n: should_copy(n, MAPPER_PREFIXES, MAPPER_EXACT))
    print("services")
    dst = COMPAT / "service"
    dst.mkdir(parents=True, exist_ok=True)
    for name in SERVICE_NAMES:
        src = BACKEND / "service" / f"{name}.java"
        if src.exists():
            shutil.copy2(src, dst / src.name)
            print(f"  service/{src.name}")
    print("controllers")
    dst = COMPAT / "controller"
    dst.mkdir(parents=True, exist_ok=True)
    for name in CONTROLLERS:
        src = BACKEND / "controller" / f"{name}.java"
        if src.exists():
            shutil.copy2(src, dst / src.name)
            print(f"  controller/{src.name}")
    print("dtos")
    copy_dir(BACKEND / "dto", COMPAT / "dto", lambda n: should_copy(n, DTO_PREFIXES, DTO_EXACT))
    print("config from order")
    for rel in CONFIG_FROM_ORDER:
        src = ORDER / rel
        dst = COMPAT / rel.replace("OrderWebConfig", "CompatWebConfig")
        if src.exists():
            dst.parent.mkdir(parents=True, exist_ok=True)
            text = src.read_text(encoding="utf-8")
            if "OrderWebConfig" in rel:
                text = text.replace("OrderWebConfig", "CompatWebConfig")
            dst.write_text(text, encoding="utf-8")
            print(f"  {dst.relative_to(COMPAT)}")
    copy_repository()
    # Mybatis config from catalog
    for rel in ["config/MybatisPlusConfig.java", "config/MyMetaObjectHandler.java"]:
        src = ROOT / "clas-catalog" / "src" / "main" / "java" / "com" / "clas" / rel
        dst = COMPAT / rel
        if src.exists():
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)
            print(f"  {rel}")
    print("done")


if __name__ == "__main__":
    main()
