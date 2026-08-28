package com.clas.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
@Data @TableName("rider_audit_log")
public class RiderAuditLog { @TableId(type = IdType.AUTO) private Long id; private String riderId; private String operatorId; private String action; private String reason; private String beforeValue; private String afterValue; private LocalDateTime createdAt; }
