package com.clas.common;

import java.util.regex.Pattern;

public final class PasswordValidator {
    public static final String MESSAGE = "密码至少6位，必须包含大小写英文字母、数字和特殊符号，且不能包含空白字符";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_])\\S{6,}$");

    private PasswordValidator() {
    }

    public static void validate(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(MESSAGE);
        }
    }
}
