package com.changzheng.auth.controller;

import com.changzheng.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 公共接口控制器
 */
@Tag(name = "公共模块")
@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Value("${sport.step-to-km-rate:2000}")
    private Integer stepToKmRate;

    @Operation(summary = "获取系统配置")
    @GetMapping("/config")
    public R<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("stepToKmRate", stepToKmRate);
        return R.ok(config);
    }

    @Operation(summary = "获取轮播图")
    @GetMapping("/banners")
    public R<Object> getBanners() {
        // 暂时返回空数组
        return R.ok(new Object[0]);
    }
}
