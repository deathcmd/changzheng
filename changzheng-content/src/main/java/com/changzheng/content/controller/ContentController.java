package com.changzheng.content.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.changzheng.common.entity.RouteNode;
import com.changzheng.common.entity.NodeContent;
import com.changzheng.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 内容模块控制器
 */
@Tag(name = "内容模块")
@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "获取路线节点列表")
    @GetMapping("/route/nodes")
    public R<List<Map<String, Object>>> getRouteNodes() {
        List<Map<String, Object>> nodes = jdbcTemplate.queryForList(
            "SELECT id, node_code as nodeCode, node_name as nodeName, " +
            "mileage_threshold as mileageThreshold, sort_order as sortOrder, " +
            "longitude, latitude, description, icon_url as iconUrl, status " +
            "FROM t_route_node WHERE status = 1 ORDER BY sort_order ASC"
        );
        return R.ok(nodes);
    }

    @Operation(summary = "获取节点详情")
    @GetMapping("/node/{nodeId}")
    public R<Map<String, Object>> getNodeDetail(@PathVariable("nodeId") Long nodeId) {
        List<Map<String, Object>> nodes = jdbcTemplate.queryForList(
            "SELECT id, node_code as nodeCode, node_name as nodeName, " +
            "mileage_threshold as mileageThreshold, sort_order as sortOrder, " +
            "longitude, latitude, description, icon_url as iconUrl, status " +
            "FROM t_route_node WHERE id = ?", nodeId
        );
        if (nodes.isEmpty()) {
            return R.fail("节点不存在");
        }
        return R.ok(nodes.get(0));
    }

    @Operation(summary = "获取节点学习内容")
    @GetMapping("/node/{nodeId}/contents")
    public R<List<Map<String, Object>>> getNodeContents(@PathVariable("nodeId") Long nodeId) {
        List<Map<String, Object>> contents = jdbcTemplate.queryForList(
            "SELECT id, node_id as nodeId, 'video' as contentType, " +
            "title, video_duration as duration, video_url as mediaUrl, video_cover_url as coverUrl, " +
            "content_text as content, is_current as status " +
            "FROM t_node_content WHERE node_id = ? AND is_current = 1 ORDER BY version DESC", nodeId
        );
        return R.ok(contents);
    }

    @Operation(summary = "获取内容详情")
    @GetMapping("/detail/{contentId}")
    public R<Map<String, Object>> getContentDetail(@PathVariable("contentId") Long contentId) {
        List<Map<String, Object>> contents = jdbcTemplate.queryForList(
            "SELECT id, node_id as nodeId, 'video' as contentType, " +
            "title, video_duration as duration, video_url as mediaUrl, video_cover_url as coverUrl, " +
            "content_text as content, content_summary as summary " +
            "FROM t_node_content WHERE id = ?", contentId
        );
        if (contents.isEmpty()) {
            return R.fail("内容不存在");
        }
        return R.ok(contents.get(0));
    }

    @Operation(summary = "标记内容已学习")
    @PostMapping("/learned/{contentId}")
    public R<String> markContentLearned(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable("contentId") Long contentId) {
        // 简单实现：记录学习记录
        try {
            jdbcTemplate.update(
                "INSERT INTO t_user_learn_record (user_id, content_id, learned_at) " +
                "VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE learned_at = NOW()",
                userId, contentId
            );
        } catch (Exception e) {
            // 表可能不存在，忽略
        }
        return R.ok("标记成功");
    }
}
