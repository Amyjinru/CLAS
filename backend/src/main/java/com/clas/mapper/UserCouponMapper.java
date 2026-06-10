package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.UserCoupon;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    @Update("""
        UPDATE user_coupon
        SET status = 'RESERVED', order_id = #{orderId}
        WHERE id = #{userCouponId}
          AND user_id = #{userId}
          AND status = 'UNUSED'
        """)
    int reserveForOrder(
        @Param("userCouponId") Long userCouponId,
        @Param("userId") String userId,
        @Param("orderId") Long orderId
    );

    @Update("""
        UPDATE user_coupon
        SET status = 'USED', used_at = #{usedAt}
        WHERE id = #{userCouponId}
          AND order_id = #{orderId}
          AND status = 'RESERVED'
        """)
    int markUsedForOrder(
        @Param("userCouponId") Long userCouponId,
        @Param("orderId") Long orderId,
        @Param("usedAt") LocalDateTime usedAt
    );

    @Update("""
        UPDATE user_coupon
        SET status = 'UNUSED', order_id = NULL
        WHERE id = #{userCouponId}
          AND status = 'RESERVED'
        """)
    int releaseReservation(@Param("userCouponId") Long userCouponId);
}
