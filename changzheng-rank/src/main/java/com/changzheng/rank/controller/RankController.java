package com.changzheng.rank.controller;

import com.changzheng.common.result.R;
import com.changzheng.rank.dto.MyRankDTO;
import com.changzheng.rank.service.RankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * 排行榜控制器
 */
@Tag(name = "排行榜", description = "排行榜相关接口")
@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
@Validated
public class RankController {

    private final RankService rankService;

    /**
     * 总榜（个人排行榜）
     * 返回：排名、真实姓名（脱敏）、微信头像、年级、班级、里程
     */
    @Operation(summary = "总榜")
    @GetMapping("/total")
    public R<Map<String, Object>> getTotalRank(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        Map<String, Object> result = rankService.getTotalRank(page, pageSize);
        return R.ok(result);
    }

    /**
     * 年级榜（用户在自己年级的排名）
     */
    @Operation(summary = "年级榜")
    @GetMapping("/grade")
    public R<Map<String, Object>> getGradeRank(
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        Map<String, Object> result = rankService.getGradeRank(userId, page, pageSize);
        return R.ok(result);
    }

    /**
     * 我的排名
     */
    @Operation(summary = "我的排名")
    @GetMapping("/my")
    public R<MyRankDTO> getMyRank(
            @RequestHeader("X-User-Id") Long userId
    ) {
        MyRankDTO result = rankService.getMyRank(userId);
        return R.ok(result);
    }
}
