package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clas.common.BusinessException;
import com.clas.config.UserContext;
import com.clas.dto.BankCardRequest;
import com.clas.dto.BankCardResponse;
import com.clas.entity.UserBankCard;
import com.clas.mapper.UserBankCardMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserBankCardService {
    private final UserBankCardMapper userBankCardMapper;

    public UserBankCardService(UserBankCardMapper userBankCardMapper) {
        this.userBankCardMapper = userBankCardMapper;
    }

    public List<BankCardResponse> listMine() {
        String userId = UserContext.getUserId();
        return userBankCardMapper.selectList(new LambdaQueryWrapper<UserBankCard>()
                .eq(UserBankCard::getUserId, userId)
                .orderByDesc(UserBankCard::getIsDefault)
                .orderByDesc(UserBankCard::getCreateTime))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public BankCardResponse create(BankCardRequest request) {
        String userId = UserContext.getUserId();
        String normalizedNo = normalizeCardNo(request.cardNo());
        boolean firstCard = userBankCardMapper.selectCount(new LambdaQueryWrapper<UserBankCard>()
            .eq(UserBankCard::getUserId, userId)) == 0;
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || firstCard;
        if (makeDefault) {
            userBankCardMapper.update(null, new LambdaUpdateWrapper<UserBankCard>()
                .eq(UserBankCard::getUserId, userId)
                .set(UserBankCard::getIsDefault, false));
        }

        UserBankCard card = new UserBankCard();
        card.setUserId(userId);
        card.setBankName(clean(request.bankName(), "开户银行", 50));
        card.setCardholderName(clean(request.cardholderName(), "持卡人", 50));
        card.setCardNoEncrypted(maskCardNo(normalizedNo));
        card.setCardLast4(normalizedNo.substring(normalizedNo.length() - 4));
        card.setCardType(cleanOptional(request.cardType(), "借记卡", 20));
        card.setIsDefault(makeDefault);
        card.setCreateTime(LocalDateTime.now());
        userBankCardMapper.insert(card);
        return toResponse(card);
    }

    public void delete(Long id) {
        String userId = UserContext.getUserId();
        int deleted = userBankCardMapper.delete(new LambdaQueryWrapper<UserBankCard>()
            .eq(UserBankCard::getId, id)
            .eq(UserBankCard::getUserId, userId));
        if (deleted != 1) {
            throw new BusinessException("银行卡不存在或无权操作");
        }
    }

    private String normalizeCardNo(String cardNo) {
        String normalized = cardNo == null ? "" : cardNo.replaceAll("\\s+", "");
        if (!normalized.matches("\\d{12,19}")) {
            throw new BusinessException("银行卡号格式不正确");
        }
        return normalized;
    }

    private String clean(String value, String label, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty()) {
            throw new BusinessException(label + "不能为空");
        }
        if (cleaned.length() > maxLength) {
            throw new BusinessException(label + "不能超过 " + maxLength + " 个字符");
        }
        return cleaned;
    }

    private String cleanOptional(String value, String fallback, int maxLength) {
        String cleaned = value == null || value.isBlank() ? fallback : value.trim();
        if (cleaned.length() > maxLength) {
            throw new BusinessException("卡类型不能超过 " + maxLength + " 个字符");
        }
        return cleaned;
    }

    private String maskCardNo(String normalizedNo) {
        return "**** **** **** " + normalizedNo.substring(normalizedNo.length() - 4);
    }

    private BankCardResponse toResponse(UserBankCard card) {
        return new BankCardResponse(
            card.getId(),
            card.getBankName(),
            card.getCardholderName(),
            card.getCardNoEncrypted(),
            card.getCardLast4(),
            card.getCardType(),
            card.getIsDefault(),
            card.getCreateTime()
        );
    }
}
