package com.changzheng.sport.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.changzheng.common.entity.DailySteps;
import com.changzheng.common.result.R;
import com.changzheng.sport.dto.ProgressVO;
import com.changzheng.sport.dto.SyncRequest;
import com.changzheng.sport.dto.SyncResult;
import com.changzheng.sport.mapper.DailyStepsMapper;
import com.changzheng.sport.service.StepSyncService;
import com.changzheng.sport.service.WxDataDecryptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 运动模块控制器
 */
@Tag(name = "运动模块")
@RestController
@RequestMapping("/api/sport")
@RequiredArgsConstructor
public class SportController {

    private final StepSyncService stepSyncService;
    private final WxDataDecryptService wxDataDecryptService;
    private final DailyStepsMapper dailyStepsMapper;

    @Operation(summary = "同步微信步数")
    @PostMapping("/syncSteps")
    public R<SyncResult> syncSteps(@RequestHeader("X-User-Id") Long userId,
                                    @Valid @RequestBody SyncRequest request) {
        // 解密微信运动数据
        JSONObject decrypted = wxDataDecryptService.decrypt(userId, request.getEncryptedData(), request.getIv());
        JSONArray stepInfoList = decrypted.getJSONArray("stepInfoList");
        
        SyncResult result = stepSyncService.syncSteps(userId, stepInfoList);
        return R.ok(result);
    }

    @Operation(summary = "获取个人进度")
    @GetMapping("/progress")
    public R<ProgressVO> getProgress(@RequestHeader("X-User-Id") Long userId) {
        ProgressVO progress = stepSyncService.getProgress(userId);
        return R.ok(progress);
    }

    @Operation(summary = "获取每日步数列表")
    @GetMapping("/dailySteps")
    public R<List<DailySteps>> getDailySteps(@RequestHeader("X-User-Id") Long userId,
                                              @RequestParam String startDate,
                                              @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<DailySteps> list = dailyStepsMapper.selectByUserIdAndDateRange(userId, start, end);
        return R.ok(list);
    }
}
