package com.clas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_bank_card")
public class UserBankCard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String bankName;
    private String cardholderName;
    private String cardNoEncrypted;
    private String cardLast4;
    private String cardType;
    private Boolean isDefault;
    private LocalDateTime createTime;
}
