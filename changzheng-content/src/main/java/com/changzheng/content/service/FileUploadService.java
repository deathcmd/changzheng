package com.changzheng.content.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.LinkOption;
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

    @Value("${file.upload.base-url}")
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

        // 创建目录，并在写入前验证目录没有通过符号链接逃逸上传根目录。
        Path root = Paths.get(basePath).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path realRoot = root.toRealPath();
        Path uploadDir = root.resolve(type).resolve(dateDir).normalize();
        Files.createDirectories(uploadDir);
        Path realUploadDir = uploadDir.toRealPath();
        if (!realUploadDir.startsWith(realRoot)) {
            throw new IOException("上传目录不合法");
        }

        // 保存文件
        Path targetPath = realUploadDir.resolve(newFilename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath);
        }

        log.info("文件上传成功: {}", newFilename);

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
        if (files == null || files.length == 0 || files.length > 10) {
            throw new IllegalArgumentException("每次最多上传10个文件");
        }
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
        if (filePath == null || filePath.isBlank() || filePath.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        Path root = Paths.get(basePath).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path path = root.resolve(filePath).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("文件路径超出上传目录");
        }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(path)) {
                throw new IllegalArgumentException("不允许删除符号链接");
            }
            Path realRoot = root.toRealPath();
            Path realParent = path.getParent().toRealPath();
            if (!realParent.startsWith(realRoot)) {
                throw new IllegalArgumentException("文件路径超出上传目录");
            }
            Files.delete(path);
            log.info("文件删除成功: {}", path.getFileName());
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

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(16);
            if (!hasAllowedSignature(header, type, extension)) {
                throw new IllegalArgumentException("文件内容与声明类型不匹配");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法读取上传文件", exception);
        }
    }

    private boolean hasAllowedSignature(byte[] bytes, String type, String extension) {
        if (bytes.length < 4) return false;
        return switch (type) {
            case "image" -> switch (extension) {
                case "jpg", "jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
                case "png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47);
                case "gif" -> ascii(bytes, 0, "GIF87a") || ascii(bytes, 0, "GIF89a");
                case "bmp" -> ascii(bytes, 0, "BM");
                case "webp" -> ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP");
                default -> false;
            };
            case "audio" -> switch (extension) {
                case "mp3" -> ascii(bytes, 0, "ID3") || startsWith(bytes, 0xFF, 0xFB)
                        || startsWith(bytes, 0xFF, 0xF3) || startsWith(bytes, 0xFF, 0xF2);
                case "wav" -> ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WAVE");
                case "ogg" -> ascii(bytes, 0, "OggS");
                case "flac" -> ascii(bytes, 0, "fLaC");
                case "m4a" -> bytes.length >= 12 && ascii(bytes, 4, "ftyp");
                case "aac" -> startsWith(bytes, 0xFF, 0xF1) || startsWith(bytes, 0xFF, 0xF9);
                default -> false;
            };
            case "video" -> switch (extension) {
                case "mp4", "mov" -> bytes.length >= 12 && ascii(bytes, 4, "ftyp");
                case "mkv", "webm" -> startsWith(bytes, 0x1A, 0x45, 0xDF, 0xA3);
                case "flv" -> ascii(bytes, 0, "FLV");
                case "avi" -> ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "AVI ");
                case "wmv" -> startsWith(bytes, 0x30, 0x26, 0xB2, 0x75);
                default -> false;
            };
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != prefix[i]) return false;
        }
        return true;
    }

    private boolean ascii(byte[] bytes, int offset, String expected) {
        byte[] value = expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (bytes.length < offset + value.length) return false;
        for (int i = 0; i < value.length; i++) {
            if (bytes[offset + i] != value[i]) return false;
        }
        return true;
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
