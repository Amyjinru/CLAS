package com.clas.common;

import java.util.regex.Pattern;

public final class PhoneValidator {
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    private PhoneValidator() {
    }

    public static String normalizeAndValidate(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("手机号不能为空");
        }
        String normalized = phone.trim();
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
        return normalized;
    }
}
