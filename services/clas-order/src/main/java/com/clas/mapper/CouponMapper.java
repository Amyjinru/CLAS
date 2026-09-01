package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
    @Update("""
        UPDATE coupon
        SET claimed_count = claimed_count + 1
        WHERE id = #{couponId}
          AND status = 'ACTIVE'
          AND (total_limit <= 0 OR claimed_count < total_limit)
        """)
    int incrementClaimedIfAvailable(@Param("couponId") Long couponId);
}
