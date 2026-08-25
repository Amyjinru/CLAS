package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField(exist = false)
    private List<String> roles;
}
