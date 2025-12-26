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
     * 查询个人排行榜
     * 使用学生认证信息（姓名、专业、班级）
     */
    @Select("""
        <script>
        SELECT 
            u.id AS userId,
            s.name AS realName,
            u.avatar_url AS avatarUrl,
            s.class_name AS className,
            s.grade AS grade,
            s.major AS major,
            u.total_mileage AS totalMileage,
            u.total_steps AS totalSteps
        FROM t_user u
        INNER JOIN t_student_info s ON u.student_no = s.student_no
        WHERE u.status = 1
        <if test="grade != null and grade != ''">
            AND s.grade = #{grade}
        </if>
        ORDER BY u.total_mileage DESC
        LIMIT #{offset}, #{pageSize}
        </script>
    """)
    List<PersonalRankDTO> selectPersonalRank(
            @Param("grade") String grade,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计个人排行榜总数（只统计已认证用户）
     */
    @Select("""
        <script>
        SELECT COUNT(1) FROM t_user u
        INNER JOIN t_student_info s ON u.student_no = s.student_no
        WHERE u.status = 1
        <if test="grade != null and grade != ''">
            AND s.grade = #{grade}
        </if>
        </script>
    """)
    int countPersonalRank(@Param("grade") String grade);

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
     * 查询班级排行榜
     */
    @Select("""
        <script>
        SELECT 
            u.class_name AS className,
            s.major AS major,
            u.grade AS grade,
            COUNT(1) AS memberCount,
            SUM(u.total_mileage) AS totalMileage,
            AVG(u.total_mileage) AS avgMileage
        FROM t_user u
        LEFT JOIN t_student_info s ON u.student_no = s.student_no
        WHERE u.status = 1 AND u.class_name IS NOT NULL
        <if test="grade != null and grade != ''">
            AND u.grade = #{grade}
        </if>
        GROUP BY u.class_name, s.major, u.grade
        ORDER BY totalMileage DESC
        LIMIT #{offset}, #{pageSize}
        </script>
    """)
    List<ClassRankDTO> selectClassRank(
            @Param("grade") String grade,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计班级排行榜总数
     */
    @Select("""
        <script>
        SELECT COUNT(DISTINCT class_name) FROM t_user
        WHERE status = 1 AND class_name IS NOT NULL
        <if test="grade != null and grade != ''">
            AND grade = #{grade}
        </if>
        </script>
    """)
    int countClassRank(@Param("grade") String grade);

    /**
     * 查询年级排行榜
     */
    @Select("""
        SELECT 
            grade,
            enroll_year AS enrollYear,
            COUNT(1) AS memberCount,
            SUM(total_mileage) AS totalMileage,
            AVG(total_mileage) AS avgMileage
        FROM t_user
        WHERE status = 1 AND grade IS NOT NULL
        GROUP BY grade, enroll_year
        ORDER BY totalMileage DESC
        LIMIT #{offset}, #{pageSize}
    """)
    List<GradeRankDTO> selectGradeRank(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计年级排行榜总数
     */
    @Select("SELECT COUNT(DISTINCT grade) FROM t_user WHERE status = 1 AND grade IS NOT NULL")
    int countGradeRank();
}
