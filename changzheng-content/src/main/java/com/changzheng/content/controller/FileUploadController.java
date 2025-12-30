package com.changzheng.content.controller;

import com.changzheng.common.result.R;
import com.changzheng.content.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/content/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 上传单个文件（图片/音频/视频）
     */
    @PostMapping("/upload")
    public R<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type) {
        try {
            Map<String, String> result = fileUploadService.uploadFile(file, type);
            return R.ok(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return R.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文件
     */
    @PostMapping("/upload/batch")
    public R<List<Map<String, String>>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "type", defaultValue = "image") String type) {
        try {
            List<Map<String, String>> results = fileUploadService.uploadFiles(files, type);
            return R.ok(results);
        } catch (Exception e) {
            log.error("批量上传失败", e);
            return R.fail("批量上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传图片
     */
    @PostMapping("/upload/image")
    public R<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "image");
    }

    /**
     * 上传音频
     */
    @PostMapping("/upload/audio")
    public R<Map<String, String>> uploadAudio(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "audio");
    }

    /**
     * 上传视频
     */
    @PostMapping("/upload/video")
    public R<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "video");
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public R<Void> deleteFile(@RequestParam("filePath") String filePath) {
        try {
            fileUploadService.deleteFile(filePath);
            return R.ok();
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return R.fail("文件删除失败: " + e.getMessage());
        }
    }
}
