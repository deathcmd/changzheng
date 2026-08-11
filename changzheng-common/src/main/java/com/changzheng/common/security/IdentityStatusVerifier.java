package com.changzheng.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Re-checks account state for every protected request so disabling an account
 * revokes already-issued access tokens without waiting for token expiry.
 */
@Component
@RequiredArgsConstructor
public class IdentityStatusVerifier {

    private final JdbcTemplate jdbcTemplate;

    public boolean isActiveStudent(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user WHERE id = ? AND status = 1", Integer.class, userId);
        return count != null && count == 1;
    }

    public boolean isActiveAdmin(long adminId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_admin WHERE id = ? AND status = 1", Integer.class, adminId);
        return count != null && count == 1;
    }
}
