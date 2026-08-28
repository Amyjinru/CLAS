package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.RiderSettlement;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface RiderSettlementMapper extends BaseMapper<RiderSettlement> {
    @Select("SELECT * FROM rider_settlement WHERE source_type = #{sourceType} AND source_id = #{sourceId} FOR UPDATE")
    List<RiderSettlement> selectBySourceForUpdate(String sourceType, String sourceId);
}
