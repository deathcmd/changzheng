package com.changzheng.admin.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsProtocolRelativeMediaUrl() {
        NodeContentRequest request = validContent();
        request.setMediaUrl("//attacker.example/video.mp4");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsHttpsAndRootRelativeMediaUrls() {
        NodeContentRequest request = validContent();
        request.setMediaUrl("https://media.example/video.mp4");
        assertTrue(validator.validate(request).isEmpty());

        request.setMediaUrl("/uploads/video.mp4");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsProtocolRelativeNodeIcon() {
        RouteNodeRequest request = new RouteNodeRequest();
        request.setNodeCode("TEST_NODE");
        request.setNodeName("测试节点");
        request.setMileageThreshold(BigDecimal.ONE);
        request.setSortOrder(1);
        request.setIconUrl("//attacker.example/icon.png");
        request.setStatus(1);

        assertFalse(validator.validate(request).isEmpty());
    }

    private NodeContentRequest validContent() {
        NodeContentRequest request = new NodeContentRequest();
        request.setContentType("video");
        request.setTitle("测试视频");
        request.setMediaUrl("/uploads/video.mp4");
        request.setSortOrder(1);
        request.setAutoPlay(false);
        request.setEnabled(true);
        return request;
    }
}
