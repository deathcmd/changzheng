package com.changzheng.content.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件上传服务
 */
@Slf4j
@Service
public class FileUploadService {

    @Value("${file.upload.base-path:/opt/changzheng/uploads}")
    private String basePath;

    @Value("${file.upload.base-url:https://deathcmd.cn/uploads}")
    private String baseUrl;

    @Value("${file.upload.max-size:104857600}")
    private long maxFileSize; // 默认100MB

    // 允许的文件类型
    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
        "image", Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp"),
        "audio", Set.of("mp3", "wav", "ogg", "m4a", "flac", "aac"),
        "video", Set.of("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm")
    );

    /**
     * 上传单个文件
     */
    public Map<String, String> uploadFile(MultipartFile file, String type) throws IOException {
        // 验证文件
        validateFile(file, type);

        // 生成文件路径
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        // 创建目录
        Path uploadDir = Paths.get(basePath, type, dateDir);
        Files.createDirectories(uploadDir);

        // 保存文件
        Path targetPath = uploadDir.resolve(newFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("文件上传成功: {}", targetPath);

        // 返回结果
        String relativePath = type + "/" + dateDir + "/" + newFilename;
        Map<String, String> result = new HashMap<>();
        result.put("filename", originalFilename);
        result.put("path", relativePath);
        result.put("url", baseUrl + "/" + relativePath);
        result.put("size", String.valueOf(file.getSize()));
        result.put("type", type);

        return result;
    }

    /**
     * 批量上传文件
     */
    public List<Map<String, String>> uploadFiles(MultipartFile[] files, String type) throws IOException {
        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                results.add(uploadFile(file, type));
            }
        }
        return results;
    }

    /**
     * 删除文件
     */
    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(basePath, filePath);
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("文件删除成功: {}", path);
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file, String type) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制（最大" + (maxFileSize / 1024 / 1024) + "MB）");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(filename).toLowerCase();
        Set<String> allowedExtensions = ALLOWED_TYPES.get(type);
        if (allowedExtensions == null || !allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}
