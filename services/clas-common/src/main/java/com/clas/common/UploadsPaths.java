package com.clas.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class UploadsPaths {
    private UploadsPaths() {
    }

    public static Path root() {
        String configured = firstNonBlank(
            System.getProperty("clas.uploads.dir"),
            System.getenv("CLAS_UPLOADS_DIR")
        );
        Path directory = configured == null
            ? Paths.get(System.getProperty("user.dir"), "uploads")
            : Paths.get(configured);
        Path normalized = directory.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
        } catch (IOException exception) {
            throw new BusinessException("创建上传目录失败");
        }
        return normalized;
    }

    public static String resourceLocation() {
        return root().toUri().toString();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
