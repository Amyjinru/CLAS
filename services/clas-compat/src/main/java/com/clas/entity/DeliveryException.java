package com.clas.entity;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import com.baomidou.mybatisplus.annotation.TableName; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("delivery_exception") public class DeliveryException { @TableId(type=IdType.AUTO) private Long id; private Long orderId; private String riderId; private String exceptionType; private String status; private Integer scoreDeduction; private Integer commissionDeduction; private String detail; private LocalDateTime createdAt; }
