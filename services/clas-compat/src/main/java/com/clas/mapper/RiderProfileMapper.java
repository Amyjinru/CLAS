package com.clas.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.RiderProfile;
import org.apache.ibatis.annotations.Select;
public interface RiderProfileMapper extends BaseMapper<RiderProfile> {
    @Select("SELECT * FROM rider_profile WHERE user_id = #{userId} FOR UPDATE")
    RiderProfile selectByUserIdForUpdate(String userId);
}
