package com.changzheng.admin.service;

import com.changzheng.admin.dto.NodeContentRequest;
import com.changzheng.admin.dto.RouteNodeRequest;
import com.changzheng.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NodeAdminService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> listNodes() {
        return jdbcTemplate.queryForList(
                "SELECT id, node_code AS nodeCode, node_name AS nodeName, " +
                        "mileage_threshold AS mileageThreshold, sort_order AS sortOrder, " +
                        "longitude, latitude, description, icon_url AS iconUrl, status " +
                        "FROM t_route_node ORDER BY sort_order ASC, id ASC");
    }

    public Map<String, Object> getNode(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, node_code AS nodeCode, node_name AS nodeName, " +
                        "mileage_threshold AS mileageThreshold, sort_order AS sortOrder, " +
                        "longitude, latitude, description, icon_url AS iconUrl, status " +
                        "FROM t_route_node WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw new BusinessException("节点不存在");
        }
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> createNode(RouteNodeRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO t_route_node " +
                                "(node_code, node_name, mileage_threshold, sort_order, longitude, latitude, " +
                                "description, icon_url, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                setNodeParameters(statement, request);
                return statement;
            }, keyHolder);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("节点编码已存在");
        }
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Node id was not generated");
        }
        return getNode(key.longValue());
    }

    @Transactional
    public Map<String, Object> updateNode(Long id, RouteNodeRequest request) {
        getNode(id);
        try {
            jdbcTemplate.update(
                    "UPDATE t_route_node SET node_code = ?, node_name = ?, mileage_threshold = ?, " +
                            "sort_order = ?, longitude = ?, latitude = ?, description = ?, icon_url = ?, " +
                            "status = ? WHERE id = ?",
                    request.getNodeCode(), request.getNodeName(), request.getMileageThreshold(),
                    request.getSortOrder(), request.getLongitude(), request.getLatitude(),
                    request.getDescription(), request.getIconUrl(), request.getStatus(), id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("节点编码已存在");
        }
        return getNode(id);
    }

    @Transactional
    public void disableNode(Long id) {
        if (jdbcTemplate.update("UPDATE t_route_node SET status = 0 WHERE id = ?", id) != 1) {
            throw new BusinessException("节点不存在");
        }
    }

    public List<Map<String, Object>> listContents(Long nodeId) {
        getNode(nodeId);
        return jdbcTemplate.queryForList(
                "SELECT id, node_id AS nodeId, content_type AS contentType, title, " +
                        "duration_label AS duration, media_url AS mediaUrl, content_text AS content, " +
                        "cover_url AS coverUrl, sort_order AS sortOrder, auto_play = 1 AS autoPlay, " +
                        "status = 1 AS enabled FROM t_node_content " +
                        "WHERE node_id = ? AND is_current = 1 ORDER BY sort_order ASC, id ASC", nodeId);
    }

    @Transactional
    public Map<String, Object> saveContent(Long nodeId, Long adminId, NodeContentRequest request) {
        getNode(nodeId);
        validateContent(request);
        if (request.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO t_node_content " +
                                "(node_id, version, title, content_type, media_url, cover_url, duration_label, " +
                                "sort_order, auto_play, status, content_text, is_current, publish_time, created_by) " +
                                "VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, NOW(), ?)",
                        Statement.RETURN_GENERATED_KEYS);
                setContentParameters(statement, nodeId, adminId, request);
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("Content id was not generated");
            }
            request.setId(key.longValue());
        } else {
            int updated = jdbcTemplate.update(
                    "UPDATE t_node_content SET title = ?, content_type = ?, media_url = ?, cover_url = ?, " +
                            "duration_label = ?, sort_order = ?, auto_play = ?, status = ?, content_text = ?, " +
                            "is_current = 1 WHERE id = ? AND node_id = ?",
                    request.getTitle(), request.getContentType(), request.getMediaUrl(), request.getCoverUrl(),
                    request.getDuration(), request.getSortOrder(), Boolean.TRUE.equals(request.getAutoPlay()),
                    Boolean.TRUE.equals(request.getEnabled()), request.getContent(), request.getId(), nodeId);
            if (updated != 1) {
                throw new BusinessException("内容不存在或不属于当前节点");
            }
        }
        return getContent(nodeId, request.getId());
    }

    @Transactional
    public void disableContent(Long nodeId, Long contentId) {
        int updated = jdbcTemplate.update(
                "UPDATE t_node_content SET status = 0, is_current = 0 WHERE id = ? AND node_id = ?",
                contentId, nodeId);
        if (updated != 1) {
            throw new BusinessException("内容不存在或不属于当前节点");
        }
    }

    private Map<String, Object> getContent(Long nodeId, Long contentId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, node_id AS nodeId, content_type AS contentType, title, " +
                        "duration_label AS duration, media_url AS mediaUrl, content_text AS content, " +
                        "cover_url AS coverUrl, sort_order AS sortOrder, auto_play = 1 AS autoPlay, " +
                        "status = 1 AS enabled FROM t_node_content WHERE id = ? AND node_id = ?",
                contentId, nodeId);
        if (rows.isEmpty()) {
            throw new BusinessException("内容不存在或不属于当前节点");
        }
        return rows.get(0);
    }

    private void validateContent(NodeContentRequest request) {
        boolean article = "article".equals(request.getContentType());
        if (article && (request.getContent() == null || request.getContent().isBlank())) {
            throw new BusinessException("文章内容不能为空");
        }
        if (!article && (request.getMediaUrl() == null || request.getMediaUrl().isBlank())) {
            throw new BusinessException("媒体地址不能为空");
        }
    }

    private void setNodeParameters(PreparedStatement statement, RouteNodeRequest request) throws java.sql.SQLException {
        statement.setString(1, request.getNodeCode());
        statement.setString(2, request.getNodeName());
        statement.setBigDecimal(3, request.getMileageThreshold());
        statement.setInt(4, request.getSortOrder());
        statement.setBigDecimal(5, request.getLongitude());
        statement.setBigDecimal(6, request.getLatitude());
        statement.setString(7, request.getDescription());
        statement.setString(8, request.getIconUrl());
        statement.setInt(9, request.getStatus());
    }

    private void setContentParameters(PreparedStatement statement, Long nodeId, Long adminId,
                                      NodeContentRequest request) throws java.sql.SQLException {
        statement.setLong(1, nodeId);
        statement.setString(2, request.getTitle());
        statement.setString(3, request.getContentType());
        statement.setString(4, request.getMediaUrl());
        statement.setString(5, request.getCoverUrl());
        statement.setString(6, request.getDuration());
        statement.setInt(7, request.getSortOrder());
        statement.setBoolean(8, Boolean.TRUE.equals(request.getAutoPlay()));
        statement.setBoolean(9, Boolean.TRUE.equals(request.getEnabled()));
        statement.setString(10, request.getContent());
        statement.setLong(11, adminId);
    }
}
