from pathlib import Path

def patch(path, replacements):
    text = Path(path).read_text(encoding='utf-8')
    for old, new in replacements:
        text = text.replace(old, new)
    Path(path).write_text(text, encoding='utf-8')

base = Path(r'd:/HuaweiMoveData/Users/troub/Desktop/CLAS 1.2/CLAS/services/clas-order/src/main/java/com/clas/service')
common = [
    ('NotificationService.NotificationTarget', 'NotificationBridge.NotificationTarget'),
    ('NotificationService notificationService', 'NotificationBridge notificationBridge'),
    ('NotificationService notifications', 'NotificationBridge notifications'),
    ('notificationService.send', 'notificationBridge.send'),
    ('notifications.send', 'notifications.send'),
    ('PenaltyService penaltyService', 'IamClient iamClient'),
    ('penaltyService.assertCanUsePlatform', 'iamClient.assertCanUsePlatform'),
    ('penaltyService.assertCanComment', 'iamClient.assertCanUsePlatform'),
    ('penaltyService.assertCanCommunicate', 'iamClient.assertCanUsePlatform'),
    ('MerchantService merchantService', 'MerchantContextService merchantContextService'),
    ('merchantService.getCurrentMerchantId', 'merchantContextService.getCurrentMerchantId'),
    ('MerchantMapper merchantMapper', 'CatalogClient catalogClient'),
    ('MerchantMapper merchants', 'CatalogClient catalogClient'),
    ('merchantMapper.selectById', 'catalogClient.getMerchant'),
    ('merchants.selectById', 'catalogClient.getMerchant'),
    ('UserMapper userMapper', 'IamClient iamClient'),
    ('RiderSettlementService settlements', 'CompatClient compatClient'),
    ('RiderSettlementService riderSettlementService', 'CompatClient compatClient'),
    ('settlements.reverseCommissionForRefund', 'compatClient.reverseCommissionForRefund'),
    ('settlements.makeCommissionWithdrawable', 'compatClient.makeCommissionWithdrawable'),
]
for name in ['ReviewService.java', 'OrderRefundDisputeService.java', 'OrderTimeoutService.java']:
    p = base / name
    if p.exists():
        patch(p, common)
        t = p.read_text(encoding='utf-8')
        t = t.replace('import com.clas.service.NotificationService;\n', 'import com.clas.client.CatalogClient;\nimport com.clas.client.CompatClient;\nimport com.clas.client.IamClient;\n')
        t = t.replace('import com.clas.service.PenaltyService;\n', '')
        t = t.replace('import com.clas.service.MerchantService;\n', '')
        t = t.replace('import com.clas.mapper.MerchantMapper;\n', '')
        t = t.replace('import com.clas.mapper.UserMapper;\n', '')
        t = t.replace('import com.clas.service.RiderSettlementService;\n', '')
        t = t.replace('import com.clas.service.NotificationService.NotificationTarget;\n', '')
        p.write_text(t, encoding='utf-8')
print('patched auxiliary services')
