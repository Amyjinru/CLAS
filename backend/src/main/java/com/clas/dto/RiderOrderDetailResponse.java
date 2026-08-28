package com.clas.dto;

import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import java.util.List;

/**
 * 骑手「订单详情」视图：订单 + 商家 + 带商品名的餐品明细。
 * 相比任务池视图（{@link RiderTaskResponse}）额外返回餐品名称，便于骑手核对取餐内容。
 */
public record RiderOrderDetailResponse(
    Orders order,
    Merchant merchant,
    List<Item> items
) {
    public record Item(Long productId, String productName, Integer quantity, Integer price) {}
}
