package com.changzheng.rank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changzheng.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 用户Mapper（排行服务使用）
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 获取用户里程
     */
    @Select("SELECT total_mileage FROM t_user WHERE id = #{userId}")
    BigDecimal selectUserMileage(@Param("userId") Long userId);
    
    /**
     * 获取用户学生认证信息（姓名、专业、班级）
     */
    @Select("""
        SELECT s.name AS realName, s.major, s.class_name AS className, s.grade,
               u.avatar_url AS avatarUrl, u.total_mileage AS totalMileage
        FROM t_user u
        INNER JOIN t_student_info s ON u.student_no = s.student_no
        WHERE u.id = #{userId}
    """)
    Map<String, Object> selectUserStudentInfo(@Param("userId") Long userId);
}
