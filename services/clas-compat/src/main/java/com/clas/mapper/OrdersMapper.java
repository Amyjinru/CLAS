package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.Orders;

/** 订单域只读。写入必须走 clas-order `/internal/order/v1` 配送命令。 */
public interface OrdersMapper extends BaseMapper<Orders> {
}
