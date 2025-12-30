package com.changzheng.common.context;

import lombok.Data;

/**
 * 当前登录用户上下文
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> USER_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户
     */
    public static void setUser(UserInfo userInfo) {
        USER_HOLDER.set(userInfo);
    }

    /**
     * 获取当前用户
     */
    public static UserInfo getUser() {
        return USER_HOLDER.get();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        UserInfo user = getUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户类型
     */
    public static String getUserType() {
        UserInfo user = getUser();
        return user != null ? user.getUserType() : null;
    }

    /**
     * 判断是否为管理员
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(getUserType());
    }

    /**
     * 清除当前用户
     */
    public static void clear() {
        USER_HOLDER.remove();
    }

    /**
     * 用户信息
     */
    @Data
    public static class UserInfo {
        private Long userId;
        private String userType;  // STUDENT / ADMIN
        private String openid;
        private String nickname;

        public UserInfo() {}

        public UserInfo(Long userId, String userType) {
            this.userId = userId;
            this.userType = userType;
        }
    }
}
