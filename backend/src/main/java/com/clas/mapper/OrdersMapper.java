package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.Orders;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface OrdersMapper extends BaseMapper<Orders> {
    @Update("""
        UPDATE orders
        SET status = #{nextStatus}
        WHERE id = #{orderId} AND status = #{expectedStatus}
        """)
    int updateStatusIfCurrent(
        @Param("orderId") Long orderId,
        @Param("expectedStatus") String expectedStatus,
        @Param("nextStatus") String nextStatus
    );
}
