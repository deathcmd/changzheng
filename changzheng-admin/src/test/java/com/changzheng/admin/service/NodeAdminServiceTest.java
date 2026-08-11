package com.changzheng.admin.service;

import com.changzheng.admin.dto.NodeContentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeAdminServiceTest {

    @Test
    void contentUpdateIsScopedToItsParentNode() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        NodeContentRequest request = new NodeContentRequest();
        request.setId(7L);
        request.setContentType("video");
        request.setTitle("测试视频");
        request.setMediaUrl("/uploads/video.mp4");
        request.setSortOrder(1);
        request.setAutoPlay(false);
        request.setEnabled(true);

        new NodeAdminService(jdbcTemplate).saveContent(9L, 42L, request);

        assertTrue(jdbcTemplate.updatedSql.contains("WHERE id = ? AND node_id = ?"));
        assertArrayEquals(new Object[] {
                "测试视频", "video", "/uploads/video.mp4", null, null, 1,
                false, true, null, 7L, 9L
        }, jdbcTemplate.updatedArguments);
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private String updatedSql;
        private Object[] updatedArguments;

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            if (sql.contains("FROM t_route_node")) {
                return List.of(Map.of("id", args[0]));
            }
            return List.of(Map.of("id", args[1], "nodeId", args[0]));
        }

        @Override
        public int update(String sql, Object... args) {
            this.updatedSql = sql;
            this.updatedArguments = args;
            return 1;
        }
    }
}
