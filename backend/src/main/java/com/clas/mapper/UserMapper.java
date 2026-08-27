package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;

public interface UserMapper extends BaseMapper<User> {
    @Update("UPDATE `user` SET session_token = #{sessionToken}, session_expires_at = #{sessionExpiresAt} WHERE phone = #{phone}")
    int updateSessionToken(@Param("phone") String phone, @Param("sessionToken") String sessionToken,
                           @Param("sessionExpiresAt") LocalDateTime sessionExpiresAt);
}

