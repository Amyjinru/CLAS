package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {
    @Update("""
        UPDATE product
        SET stock = stock - #{quantity}
        WHERE id = #{productId} AND stock >= #{quantity}
        """)
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}

