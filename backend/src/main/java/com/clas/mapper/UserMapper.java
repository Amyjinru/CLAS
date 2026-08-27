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

    /** 退出登录时清除指定会话。带 sessionToken 条件，避免把已覆盖旧会话的新设备一并踢下线。 */
    @Update("UPDATE `user` SET session_token = NULL, session_expires_at = NULL WHERE phone = #{phone} AND session_token = #{sessionToken}")
    int clearSessionToken(@Param("phone") String phone, @Param("sessionToken") String sessionToken);
}

