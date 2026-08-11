package com.changzheng.content.controller;

import com.changzheng.common.exception.BusinessException;
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
            "FROM t_route_node WHERE id = ? AND status = 1", nodeId
        );
        if (nodes.isEmpty()) {
            return R.fail("节点不存在");
        }
        return R.ok(nodes.get(0));
    }

    @Operation(summary = "获取节点学习内容")
    @GetMapping("/node/{nodeId}/contents")
    public R<List<Map<String, Object>>> getNodeContents(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("nodeId") Long nodeId) {
        requireUnlockedNode(userId, nodeId);
        List<Map<String, Object>> contents = jdbcTemplate.queryForList(
            "SELECT id, node_id as nodeId, content_type as contentType, " +
            "title, duration_label as duration, media_url as mediaUrl, cover_url as coverUrl, " +
            "content_text as content, status " +
            "FROM t_node_content WHERE node_id = ? AND is_current = 1 AND status = 1 " +
            "ORDER BY sort_order ASC, id ASC", nodeId
        );
        return R.ok(contents);
    }

    @Operation(summary = "获取内容详情")
    @GetMapping("/detail/{contentId}")
    public R<Map<String, Object>> getContentDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("contentId") Long contentId) {
        List<Map<String, Object>> contents = jdbcTemplate.queryForList(
            "SELECT c.id, c.node_id as nodeId, c.content_type as contentType, " +
            "c.title, c.duration_label as duration, c.media_url as mediaUrl, c.cover_url as coverUrl, " +
            "c.content_text as content, c.content_summary as summary " +
            "FROM t_node_content c INNER JOIN t_user_node_progress p " +
            "ON p.node_id = c.node_id AND p.user_id = ? AND p.unlock_status = 1 " +
            "INNER JOIN t_user u ON u.id = p.user_id AND u.status = 1 " +
            "WHERE c.id = ? AND c.is_current = 1 AND c.status = 1", userId, contentId
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
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_node_content c INNER JOIN t_user_node_progress p " +
                        "ON p.node_id = c.node_id AND p.user_id = ? AND p.unlock_status = 1 " +
                        "INNER JOIN t_user u ON u.id = p.user_id AND u.status = 1 " +
                        "WHERE c.id = ? AND c.is_current = 1 AND c.status = 1",
                Integer.class, userId, contentId);
        if (count == null || count == 0) {
            throw new BusinessException("内容不存在或已禁用");
        }
        jdbcTemplate.update(
            "INSERT INTO t_user_learn_record (user_id, content_id, learned_at) " +
            "VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE learned_at = NOW()",
            userId, contentId
        );
        return R.ok("标记成功");
    }

    private void requireUnlockedNode(Long userId, Long nodeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user_node_progress p " +
                        "INNER JOIN t_user u ON u.id = p.user_id AND u.status = 1 " +
                        "WHERE p.user_id = ? AND p.node_id = ? AND p.unlock_status = 1",
                Integer.class, userId, nodeId);
        if (count == null || count == 0) {
            throw new BusinessException(com.changzheng.common.result.ResultCode.NODE_NOT_UNLOCKED);
        }
    }
}
