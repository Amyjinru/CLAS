package com.clas.dto;

import com.clas.entity.DealRedeemLog;
import java.time.LocalDateTime;

public record DealRedeemLogResponse(
    Long id,
    Long dealOrderId,
    String voucherCode,
    LocalDateTime redeemedAt
) {
    public static DealRedeemLogResponse from(DealRedeemLog log) {
        return new DealRedeemLogResponse(
            log.getId(),
            log.getDealOrderId(),
            log.getVoucherCode(),
            log.getRedeemedAt()
        );
    }
}
