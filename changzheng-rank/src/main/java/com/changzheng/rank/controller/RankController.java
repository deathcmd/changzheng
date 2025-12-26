package com.changzheng.rank.controller;

import com.changzheng.common.context.UserContext;
import com.changzheng.common.result.R;
import com.changzheng.rank.dto.MyRankDTO;
import com.changzheng.rank.service.RankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 排行榜控制器
 */
@Tag(name = "排行榜", description = "排行榜相关接口")
@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    /**
     * 个人排行榜
     * 返回：排名、真实姓名（脱敏）、微信头像、专业、班级、里程
     */
    @Operation(summary = "个人排行榜")
    @GetMapping("/personal")
    public R<Map<String, Object>> getPersonalRank(
            @Parameter(description = "年级筛选") @RequestParam(required = false) String grade,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize
    ) {
        Map<String, Object> result = rankService.getPersonalRank(grade, page, pageSize);
        return R.ok(result);
    }

    /**
     * 班级排行榜
     * 返回：排名、班级名称、专业、人数、总里程、人均里程
     */
    @Operation(summary = "班级排行榜")
    @GetMapping("/class")
    public R<Map<String, Object>> getClassRank(
            @Parameter(description = "年级筛选") @RequestParam(required = false) String grade,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize
    ) {
        Map<String, Object> result = rankService.getClassRank(grade, page, pageSize);
        return R.ok(result);
    }

    /**
     * 年级排行榜
     * 返回：排名、年级、人数、总里程、人均里程
     */
    @Operation(summary = "年级排行榜")
    @GetMapping("/grade")
    public R<Map<String, Object>> getGradeRank(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize
    ) {
        Map<String, Object> result = rankService.getGradeRank(page, pageSize);
        return R.ok(result);
    }

    /**
     * 我的排名
     * 返回：排名、里程、真实姓名（脱敏）、头像、专业、班级
     */
    @Operation(summary = "我的排名")
    @GetMapping("/my")
    public R<MyRankDTO> getMyRank(
            @Parameter(description = "排行类型：personal/class/grade") @RequestParam(defaultValue = "personal") String type
    ) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        MyRankDTO result = rankService.getMyRank(userId, type);
        return R.ok(result);
    }
}
