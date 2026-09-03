package com.clas.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorage {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp"
    );

    public Path resolveDirectory(String... segments) {
        Path base = UploadsPaths.root();
        for (String segment : segments) {
            base = base.resolve(segment);
        }
        Path directory = base.toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new BusinessException("创建上传目录失败");
        }
        return directory;
    }

    public String store(MultipartFile file, Path directory, String defaultExtension) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "file" + defaultExtension : file.getOriginalFilename();
        String extension = original.contains(".")
            ? original.substring(original.lastIndexOf('.')).toLowerCase()
            : defaultExtension;
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的文件格式，仅允许: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        String filename = UUID.randomUUID() + extension;
        Path target = directory.resolve(filename).toAbsolutePath().normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("图片上传失败");
        }
        Path uploadsRoot = UploadsPaths.root();
        return "/uploads/" + uploadsRoot.relativize(target).toString().replace('\\', '/');
    }
}
