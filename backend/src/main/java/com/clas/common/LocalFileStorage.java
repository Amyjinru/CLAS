package com.clas.common;

import com.clas.common.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorage {
    public Path resolveDirectory(String... segments) {
        Path base = Paths.get(System.getProperty("user.dir"), "uploads");
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
            ? original.substring(original.lastIndexOf('.'))
            : defaultExtension;
        String filename = UUID.randomUUID() + extension;
        Path target = directory.resolve(filename).toAbsolutePath().normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("图片上传失败");
        }
        Path uploadsRoot = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        return "/uploads/" + uploadsRoot.relativize(target).toString().replace('\\', '/');
    }
}
