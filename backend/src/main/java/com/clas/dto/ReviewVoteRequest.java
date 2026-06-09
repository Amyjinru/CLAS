package com.clas.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewVoteRequest(
    @NotBlank String targetType,
    @NotBlank String voteType
) {
}
