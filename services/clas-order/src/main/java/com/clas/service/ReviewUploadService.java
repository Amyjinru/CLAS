package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.common.LocalFileStorage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReviewUploadService {
    private static final int MAX_FILES = 9;
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final LocalFileStorage localFileStorage;

    public ReviewUploadService(LocalFileStorage localFileStorage) {
        this.localFileStorage = localFileStorage;
    }

    public List<String> upload(MultipartFile[] files, String userId) {
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (files.length > MAX_FILES) {
            throw new BusinessException("单次最多上传 " + MAX_FILES + " 张图片");
        }
        Path uploadDir = localFileStorage.resolveDirectory("reviews", userId);
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_SIZE) {
                throw new BusinessException("单张图片不能超过 5MB");
            }
            urls.add(localFileStorage.store(file, uploadDir, ".jpg"));
        }
        if (urls.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        return urls;
    }
}
