package com.clas.dto;

import jakarta.validation.constraints.NotNull;

public record RiderOnlineRequest(@NotNull Boolean online) {
}
