package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@TableName("`user`")
public class User {
    @TableId(type = IdType.INPUT)
    private String phone;
    private String username;
    private String password;
    private String role;
    private Boolean enabled;
    private String avatar;
    private String nickname;
    @JsonIgnore
    private String sessionToken;
    @JsonIgnore
    private LocalDateTime sessionExpiresAt;
    @JsonIgnore
    private String sessionDeviceId;
    @JsonIgnore
    private LocalDateTime sessionLastSeenAt;
    @JsonIgnore
    private String pendingLoginChallengeId;
    @JsonIgnore
    private String pendingLoginDeviceId;
    @JsonIgnore
    private LocalDateTime pendingLoginCreatedAt;
    @TableField(exist = false)
    private List<String> roles;
}
