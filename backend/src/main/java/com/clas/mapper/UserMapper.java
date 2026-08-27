package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;

public interface UserMapper extends BaseMapper<User> {
    @Update("UPDATE `user` SET session_token = #{sessionToken}, session_expires_at = #{sessionExpiresAt}, "
        + "session_device_id = #{deviceId}, session_last_seen_at = #{lastSeenAt}, "
        + "pending_login_challenge_id = NULL, pending_login_device_id = NULL, pending_login_created_at = NULL "
        + "WHERE phone = #{phone}")
    int updateSessionToken(@Param("phone") String phone, @Param("sessionToken") String sessionToken,
                           @Param("sessionExpiresAt") LocalDateTime sessionExpiresAt,
                           @Param("deviceId") String deviceId, @Param("lastSeenAt") LocalDateTime lastSeenAt);

    /** 退出登录时清除指定会话。带 sessionToken 条件，避免把已覆盖旧会话的新设备一并踢下线。 */
    @Update("UPDATE `user` SET session_token = NULL, session_expires_at = NULL, session_device_id = NULL, session_last_seen_at = NULL, "
        + "pending_login_challenge_id = NULL, pending_login_device_id = NULL, pending_login_created_at = NULL "
        + "WHERE phone = #{phone} AND session_token = #{sessionToken}")
    int clearSessionToken(@Param("phone") String phone, @Param("sessionToken") String sessionToken);

    @Update("UPDATE `user` SET session_last_seen_at = #{lastSeenAt} WHERE phone = #{phone} AND session_token = #{sessionToken}")
    int touchSession(@Param("phone") String phone, @Param("sessionToken") String sessionToken,
                     @Param("lastSeenAt") LocalDateTime lastSeenAt);

    @Update("UPDATE `user` SET pending_login_challenge_id = #{challengeId}, pending_login_device_id = #{deviceId}, "
        + "pending_login_created_at = #{createdAt} WHERE phone = #{phone} AND session_token = #{sessionToken}")
    int createLoginChallenge(@Param("phone") String phone, @Param("sessionToken") String sessionToken,
                             @Param("challengeId") String challengeId, @Param("deviceId") String deviceId,
                             @Param("createdAt") LocalDateTime createdAt);
}

