package com.clas.entity;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import com.baomidou.mybatisplus.annotation.TableName; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("rider_withdrawal") public class RiderWithdrawal { @TableId(type=IdType.AUTO) private Long id; private String riderId; private Long bankCardId; private Integer amount; private String status; private String reviewerId; private String reviewReason; private LocalDateTime createdAt; private LocalDateTime reviewedAt; }
