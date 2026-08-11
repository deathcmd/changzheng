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

    /**
     * User-controlled upload metadata is converted to fixed constants before it
     * can become part of a filesystem path.
     */
    private enum UploadType {
        IMAGE("image"),
        AUDIO("audio"),
        VIDEO("video");

        private final String directory;

        UploadType(String directory) {
            this.directory = directory;
        }

        String directory() {
            return directory;
        }

        static UploadType from(String value) {
            return switch (value == null ? "" : value) {
                case "image" -> IMAGE;
                case "audio" -> AUDIO;
                case "video" -> VIDEO;
                default -> throw new IllegalArgumentException("不支持的文件分类");
            };
        }
    }

    private enum UploadExtension {
        JPG(".jpg", UploadType.IMAGE),
        JPEG(".jpeg", UploadType.IMAGE),
        PNG(".png", UploadType.IMAGE),
        GIF(".gif", UploadType.IMAGE),
        WEBP(".webp", UploadType.IMAGE),
        BMP(".bmp", UploadType.IMAGE),
        MP3(".mp3", UploadType.AUDIO),
        WAV(".wav", UploadType.AUDIO),
        OGG(".ogg", UploadType.AUDIO),
        M4A(".m4a", UploadType.AUDIO),
        FLAC(".flac", UploadType.AUDIO),
        AAC(".aac", UploadType.AUDIO),
        MP4(".mp4", UploadType.VIDEO),
        AVI(".avi", UploadType.VIDEO),
        MOV(".mov", UploadType.VIDEO),
        WMV(".wmv", UploadType.VIDEO),
        FLV(".flv", UploadType.VIDEO),
        MKV(".mkv", UploadType.VIDEO),
        WEBM(".webm", UploadType.VIDEO);

        private final String suffix;
        private final UploadType type;

        UploadExtension(String suffix, UploadType type) {
            this.suffix = suffix;
            this.type = type;
        }

        String suffix() {
            return suffix;
        }

        static UploadExtension from(String filename, UploadType expectedType) {
            String extension = getFileExtension(filename).toLowerCase(Locale.ROOT);
            UploadExtension result = switch (extension) {
                case "jpg" -> JPG;
                case "jpeg" -> JPEG;
                case "png" -> PNG;
                case "gif" -> GIF;
                case "webp" -> WEBP;
                case "bmp" -> BMP;
                case "mp3" -> MP3;
                case "wav" -> WAV;
                case "ogg" -> OGG;
                case "m4a" -> M4A;
                case "flac" -> FLAC;
                case "aac" -> AAC;
                case "mp4" -> MP4;
                case "avi" -> AVI;
                case "mov" -> MOV;
                case "wmv" -> WMV;
                case "flv" -> FLV;
                case "mkv" -> MKV;
                case "webm" -> WEBM;
                default -> throw new IllegalArgumentException("不支持的文件扩展名");
            };
            if (result.type != expectedType) {
                throw new IllegalArgumentException("文件扩展名与上传分类不匹配");
            }
            return result;
        }
    }

    /**
     * 上传单个文件
     */
    public Map<String, String> uploadFile(MultipartFile file, String type) throws IOException {
        UploadType uploadType = UploadType.from(type);
        UploadExtension extension = validateFile(file, uploadType);

        // 生成文件路径
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String originalFilename = safeDisplayFilename(file.getOriginalFilename());
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension.suffix();

        // 创建目录，并在写入前验证目录没有通过符号链接逃逸上传根目录。
        Path root = Paths.get(basePath).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path realRoot = root.toRealPath();
        Path uploadDir = root.resolve(uploadType.directory()).resolve(dateDir).normalize();
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
        String relativePath = uploadType.directory() + "/" + dateDir + "/" + newFilename;
        Map<String, String> result = new HashMap<>();
        result.put("filename", originalFilename);
        result.put("path", relativePath);
        result.put("url", baseUrl + "/" + relativePath);
        result.put("size", String.valueOf(file.getSize()));
        result.put("type", uploadType.directory());

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
    private UploadExtension validateFile(MultipartFile file, UploadType type) {
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

        UploadExtension extension = UploadExtension.from(filename, type);

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(16);
            if (!hasAllowedSignature(header, type, extension)) {
                throw new IllegalArgumentException("文件内容与声明类型不匹配");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法读取上传文件", exception);
        }
        return extension;
    }

    private boolean hasAllowedSignature(byte[] bytes, UploadType type, UploadExtension extension) {
        if (bytes.length < 4) return false;
        return switch (type) {
            case IMAGE -> switch (extension) {
                case JPG, JPEG -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
                case PNG -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47);
                case GIF -> ascii(bytes, 0, "GIF87a") || ascii(bytes, 0, "GIF89a");
                case BMP -> ascii(bytes, 0, "BM");
                case WEBP -> ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP");
                default -> false;
            };
            case AUDIO -> switch (extension) {
                case MP3 -> ascii(bytes, 0, "ID3") || startsWith(bytes, 0xFF, 0xFB)
                        || startsWith(bytes, 0xFF, 0xF3) || startsWith(bytes, 0xFF, 0xF2);
                case WAV -> ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WAVE");
                case OGG -> ascii(bytes, 0, "OggS");
                case FLAC -> ascii(bytes, 0, "fLaC");
                case M4A -> bytes.length >= 12 && ascii(bytes, 4, "ftyp");
                case AAC -> startsWith(bytes, 0xFF, 0xF1) || startsWith(bytes, 0xFF, 0xF9);
                default -> false;
            };
            case VIDEO -> switch (extension) {
                case MP4, MOV -> bytes.length >= 12 && ascii(bytes, 4, "ftyp");
                case MKV, WEBM -> startsWith(bytes, 0x1A, 0x45, 0xDF, 0xA3);
                case FLV -> ascii(bytes, 0, "FLV");
                case AVI -> ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "AVI ");
                case WMV -> startsWith(bytes, 0x30, 0x26, 0xB2, 0x75);
                default -> false;
            };
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
    private static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    private String safeDisplayFilename(String filename) {
        String normalized = StringUtils.cleanPath(filename.replace('\\', '/'));
        String basename = StringUtils.getFilename(normalized);
        return basename == null ? "upload" : basename;
    }
}
