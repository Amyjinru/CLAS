package com.clas.entity;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import com.baomidou.mybatisplus.annotation.TableName; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("rider_review") public class RiderReview { @TableId(type=IdType.AUTO) private Long id; private Long orderId; private String userId; private String riderId; private Integer score; private String tags; private String content; private LocalDateTime createdAt; }
