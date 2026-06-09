package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.GroupDeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GroupDealMapper extends BaseMapper<GroupDeal> {
    @Update("""
        UPDATE group_deal
        SET stock = stock - 1
        WHERE id = #{dealId} AND stock >= 1 AND status = 'ON_SALE'
        """)
    int deductStock(@Param("dealId") Long dealId);

    @Update("""
        UPDATE group_deal
        SET stock = stock + 1
        WHERE id = #{dealId}
        """)
    int restoreStock(@Param("dealId") Long dealId);
}
