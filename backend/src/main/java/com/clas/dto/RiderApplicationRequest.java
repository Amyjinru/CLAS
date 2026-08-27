package com.clas.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record RiderApplicationRequest(
    @NotBlank String realName,
    @NotBlank @Pattern(regexp = "^[0-9Xx]{18}$", message = "身份证号格式不正确") String idCardNo,
    @NotBlank String vehicleType, @NotBlank String serviceArea,
    @NotBlank String emergencyContactName,
    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "紧急联系人手机号格式不正确") String emergencyContactPhone,
    String credentialUrls) {}
