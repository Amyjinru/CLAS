package com.clas.dto;

import java.util.List;

public record InternalPage<T>(
    List<T> records,
    long total,
    long page,
    long size
) {
}
