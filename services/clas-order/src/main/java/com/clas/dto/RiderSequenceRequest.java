package com.clas.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RiderSequenceRequest(@NotEmpty List<Long> orderIds) {
}
