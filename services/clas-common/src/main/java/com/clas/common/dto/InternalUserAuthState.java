package com.clas.common.dto;

import java.util.List;

/** Minimal IAM-owned state required to validate a user token in another service. */
public record InternalUserAuthState(
    String userId,
    String username,
    String primaryRole,
    Boolean enabled,
    String sessionToken,
    List<String> approvedOrLegacyRoles,
    boolean accountOnlyRestricted
) {
}
