package com.changzheng.content.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUploadServiceTest {

    @TempDir
    Path uploadRoot;

    private FileUploadService service;

    @BeforeEach
    void setUp() {
        service = new FileUploadService();
        ReflectionTestUtils.setField(service, "basePath", uploadRoot.toString());
        ReflectionTestUtils.setField(service, "baseUrl", "https://files.example.test/uploads");
        ReflectionTestUtils.setField(service, "maxFileSize", 1024L);
    }

    @Test
    void acceptsPngWithMatchingSignature() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", png);

        var result = service.uploadFile(file, "image");

        assertTrue(result.get("path").endsWith(".png"));
        assertEquals("https://files.example.test/uploads/" + result.get("path"), result.get("url"));
    }

    @Test
    void rejectsExtensionAndContentMismatch() {
        byte[] jpeg = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "disguised.png", "image/png", jpeg);

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(file, "image"));
    }

    @Test
    void rejectsDeletionOutsideUploadRoot() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("../outside.txt"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("..\\outside.txt"));
    }
}
