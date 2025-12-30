package com.changzheng.rank.mapper;

import com.changzheng.rank.dto.ClassRankDTO;
import com.changzheng.rank.dto.GradeRankDTO;
import com.changzheng.rank.dto.PersonalRankDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 排行榜Mapper
 */
@Mapper
public interface RankMapper {

    /**
     * 查询总榜（个人排行榜）
     */
    @Select("""
        SELECT 
            u.id AS userId,
            u.name AS realName,
            u.avatar_url AS avatarUrl,
            u.class_name AS className,
            u.grade AS grade,
            u.college AS major,
            u.total_mileage AS totalMileage,
            u.total_steps AS totalSteps
        FROM t_user u
        WHERE u.status = 1 AND u.student_no IS NOT NULL
        ORDER BY u.total_mileage DESC
        LIMIT #{offset}, #{pageSize}
    """)
    List<PersonalRankDTO> selectTotalRank(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计总榜总数
     */
    @Select("SELECT COUNT(1) FROM t_user WHERE status = 1 AND student_no IS NOT NULL")
    int countTotalRank();

    /**
     * 查询用户排名
     */
    @Select("""
        SELECT COUNT(1) + 1 AS rank
        FROM t_user
        WHERE status = 1 AND total_mileage > (
            SELECT total_mileage FROM t_user WHERE id = #{userId}
        )
    """)
    Integer selectUserRank(@Param("userId") Long userId);


    /**
     * 按年级查询排行榜
     */
    @Select("""
        SELECT 
            u.id AS userId,
            u.name AS realName,
            u.avatar_url AS avatarUrl,
            u.class_name AS className,
            u.grade AS grade,
            u.college AS major,
            u.total_mileage AS totalMileage,
            u.total_steps AS totalSteps
        FROM t_user u
        WHERE u.status = 1 AND u.student_no IS NOT NULL AND u.grade = #{grade}
        ORDER BY u.total_mileage DESC
        LIMIT #{offset}, #{pageSize}
    """)
    List<PersonalRankDTO> selectRankByGrade(
            @Param("grade") String grade,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计年级排行榜总数
     */
    @Select("SELECT COUNT(1) FROM t_user WHERE status = 1 AND student_no IS NOT NULL AND grade = #{grade}")
    int countRankByGrade(@Param("grade") String grade);
}
