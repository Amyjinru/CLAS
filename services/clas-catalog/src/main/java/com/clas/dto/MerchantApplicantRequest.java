package com.clas.dto;

public record MerchantApplicantRequest(
    String accountPhone,
    String password,
    String confirmPassword,
    String username,
    String code,
    String loggedInUserId
) {
}
