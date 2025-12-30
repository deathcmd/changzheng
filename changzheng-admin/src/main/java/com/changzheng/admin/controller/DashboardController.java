package com.changzheng.admin.controller;

import com.changzheng.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 仪表盘控制器
 */
@Tag(name = "数据看板")
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 总用户数
            Integer totalUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user WHERE status = 1", Integer.class);
            stats.put("totalUsers", totalUsers != null ? totalUsers : 0);
            
            // 今日活跃用户（今天有步数记录的用户）
            Integer activeUsersToday = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM t_daily_steps WHERE record_date = CURDATE()", 
                Integer.class);
            stats.put("activeUsersToday", activeUsersToday != null ? activeUsersToday : 0);
            
            // 平均里程（从t_user表获取）
            Double avgMileage = jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(total_mileage), 0) FROM t_user WHERE status = 1", Double.class);
            stats.put("averageMileage", avgMileage != null ? Math.round(avgMileage * 10) / 10.0 : 0);
            
            // 完成率（完成长征的用户 / 总用户，长征总里程25000km）
            Integer completedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user WHERE total_mileage >= 25000 AND status = 1", Integer.class);
            double completionRate = 0;
            if (totalUsers != null && totalUsers > 0) {
                completionRate = (completedCount != null ? completedCount : 0) * 1.0 / totalUsers;
            }
            stats.put("completionRate", Math.round(completionRate * 1000) / 1000.0);
            
            // 每日活跃用户趋势（最近7天）
            List<Map<String, Object>> dailyActiveStats = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM t_daily_steps WHERE record_date = ?",
                    Integer.class, date);
                Map<String, Object> item = new HashMap<>();
                item.put("date", date.format(formatter));
                item.put("count", count != null ? count : 0);
                dailyActiveStats.add(item);
            }
            stats.put("dailyActiveStats", dailyActiveStats);
            
            // 节点解锁Top10（从t_user_node_progress表统计）
            List<Map<String, Object>> nodeClickStats = new ArrayList<>();
            try {
                List<Map<String, Object>> nodes = jdbcTemplate.queryForList(
                    "SELECT n.node_name as nodeName, COUNT(up.id) as viewCount " +
                    "FROM t_route_node n " +
                    "LEFT JOIN t_user_node_progress up ON n.id = up.node_id AND up.unlock_status = 1 " +
                    "WHERE n.status = 1 " +
                    "GROUP BY n.id, n.node_name " +
                    "ORDER BY viewCount DESC " +
                    "LIMIT 10");
                nodeClickStats.addAll(nodes);
            } catch (Exception e) {
                // 如果查询失败，返回空列表
            }
            stats.put("nodeClickStats", nodeClickStats);
            
        } catch (Exception e) {
            // 表可能不存在，返回默认值
            stats.put("totalUsers", 0);
            stats.put("activeUsersToday", 0);
            stats.put("averageMileage", 0);
            stats.put("completionRate", 0);
            stats.put("dailyActiveStats", Collections.emptyList());
            stats.put("nodeClickStats", Collections.emptyList());
        }
        
        return R.ok(stats);
    }
}

