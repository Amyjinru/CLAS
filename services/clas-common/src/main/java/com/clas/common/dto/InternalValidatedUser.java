package com.clas.common.dto;

import java.util.List;

/** Non-sensitive identity returned only after IAM validates the supplied token. */
public record InternalValidatedUser(
    String userId,
    String username,
    String activeRole,
    List<String> roles,
    boolean accountOnlyRestricted
) {
}
