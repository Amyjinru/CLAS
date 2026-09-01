package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.Payment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PaymentMapper extends BaseMapper<Payment> {
    @Update("""
        UPDATE payment
        SET status = #{nextStatus}
        WHERE id = #{paymentId} AND status = #{expectedStatus}
        """)
    int updateStatusIfCurrent(
        @Param("paymentId") Long paymentId,
        @Param("expectedStatus") String expectedStatus,
        @Param("nextStatus") String nextStatus
    );
}
