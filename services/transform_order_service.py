from pathlib import Path

src = Path(r'd:/HuaweiMoveData/Users/troub/Desktop/CLAS 1.2/CLAS/backend/src/main/java/com/clas/service/OrderService.java')
dst = Path(r'd:/HuaweiMoveData/Users/troub/Desktop/CLAS 1.2/CLAS/services/clas-order/src/main/java/com/clas/service/OrderService.java')
text = src.read_text(encoding='utf-8')
replacements = [
    ('import com.clas.entity.UserAddress;\n', ''),
    ('import com.clas.mapper.MerchantMapper;\n', ''),
    ('import com.clas.mapper.ProductMapper;\n', ''),
    ('import com.clas.mapper.UserAddressMapper;\n', ''),
    ('import com.clas.service.NotificationService;\n', 'import com.clas.client.CatalogClient;\nimport com.clas.client.CompatClient;\nimport com.clas.client.IamClient;\nimport com.clas.config.UserContext;\nimport com.clas.dto.InternalAddressResponse;\n'),
    ('import com.clas.service.PenaltyService;\n', ''),
    ('import com.clas.service.MerchantService;\n', ''),
    ('import com.clas.service.RiderSettlementService;\n', ''),
    ('NotificationService.NotificationTarget', 'NotificationBridge.NotificationTarget'),
    ('private final ProductMapper productMapper;\n    private final NotificationService notificationService;\n    private final MerchantMapper merchantMapper;\n    private final UserAddressMapper userAddressMapper;\n    private final AmapRouteService amapRouteService;\n    private final PenaltyService penaltyService;\n    private final CouponService couponService;\n    private final MerchantService merchantService;\n    private final RiderSettlementService riderSettlementService;\n',
     'private final CatalogClient catalogClient;\n    private final NotificationBridge notificationBridge;\n    private final IamClient iamClient;\n    private final AmapRouteService amapRouteService;\n    private final CouponService couponService;\n    private final CompatClient compatClient;\n'),
    ('ProductMapper productMapper,\n        NotificationService notificationService,\n        MerchantMapper merchantMapper,\n        UserAddressMapper userAddressMapper,\n        AmapRouteService amapRouteService,\n        PenaltyService penaltyService,\n        CouponService couponService,\n        MerchantService merchantService,\n        RiderSettlementService riderSettlementService,\n',
     'CatalogClient catalogClient,\n        NotificationBridge notificationBridge,\n        IamClient iamClient,\n        AmapRouteService amapRouteService,\n        CouponService couponService,\n        CompatClient compatClient,\n'),
    ('        this.productMapper = productMapper;\n        this.notificationService = notificationService;\n        this.merchantMapper = merchantMapper;\n        this.userAddressMapper = userAddressMapper;\n        this.amapRouteService = amapRouteService;\n        this.penaltyService = penaltyService;\n        this.couponService = couponService;\n        this.merchantService = merchantService;\n        this.riderSettlementService = riderSettlementService;\n',
     '        this.catalogClient = catalogClient;\n        this.notificationBridge = notificationBridge;\n        this.iamClient = iamClient;\n        this.amapRouteService = amapRouteService;\n        this.couponService = couponService;\n        this.compatClient = compatClient;\n'),
    ('penaltyService.assertCanUsePlatform', 'iamClient.assertCanUsePlatform'),
    ('notificationService.send', 'notificationBridge.send'),
    ('merchantService.refreshAveragePrice', 'catalogClient.refreshAveragePrice'),
    ('riderSettlementService.', 'compatClient.'),
    ('productMapper.selectBatchIds(productIds).stream()\n            .collect(Collectors.toMap(Product::getId, product -> product))', 'catalogClient.getProducts(productIds)'),
    ('productMapper.selectBatchIds(productIds).stream()\n                .collect(Collectors.toMap(Product::getId, product -> product))', 'catalogClient.getProducts(productIds)'),
    ('merchantMapper.selectBatchIds(merchantIds).stream()\n                .collect(Collectors.toMap(Merchant::getId, merchant -> merchant))', 'catalogClient.getMerchants(merchantIds)'),
    ('productMapper.restoreStock', 'catalogClient.restoreProductStock'),
    ('productMapper.deductStock', 'catalogClient.deductProductStock'),
    ('productMapper.selectById', 'catalogClient.getProduct'),
    ('merchantMapper.selectById', 'catalogClient.getMerchant'),
]
for old, new in replacements:
    text = text.replace(old, new)
text = text.replace(
    '            UserAddress address = userAddressMapper.selectById(request.addressId());\n            if (address == null || !request.userId().equals(address.getUserId())) {\n                throw new BusinessException("地址不存在或无权操作");\n            }\n            if (!GeoUtils.hasCoordinate(address.getLongitude(), address.getLatitude())) {\n                throw new BusinessException("该地址缺少地图坐标");\n            }\n            addressText = address.getAddress();\n            contactName = firstNonBlank(request.deliveryContactName(), address.getContactName());\n            contactPhone = firstNonBlank(request.deliveryContactPhone(), address.getPhone());\n            longitude = address.getLongitude();\n            latitude = address.getLatitude();',
    '            InternalAddressResponse address = iamClient.getAddress(request.addressId(), request.userId());\n            if (address == null) {\n                throw new BusinessException("地址不存在或无权操作");\n            }\n            if (!GeoUtils.hasCoordinate(address.longitude(), address.latitude())) {\n                throw new BusinessException("该地址缺少地图坐标");\n            }\n            addressText = firstNonBlank(request.deliveryAddress(), "");\n            contactName = firstNonBlank(request.deliveryContactName(), "");\n            contactPhone = firstNonBlank(request.deliveryContactPhone(), "");\n            longitude = address.longitude();\n            latitude = address.latitude();'
)
text = text.replace(
    '            int rows = catalogClient.deductProductStock(item.getProductId(), item.getQuantity());\n            if (rows == 0) {',
    '            if (!catalogClient.deductProductStock(item.getProductId(), item.getQuantity())) {'
)
dst.write_text(text, encoding='utf-8')
print('done')
