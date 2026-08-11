package com.changzheng.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.changzheng.common.entity.StudentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 学生信息Mapper（管理后台）
 */
@Mapper
public interface StudentInfoMapper extends BaseMapper<StudentInfo> {

    /**
     * 根据学号查询
     */
    @Select("SELECT * FROM t_student_info WHERE student_no = #{studentNo}")
    StudentInfo selectByStudentNo(@Param("studentNo") String studentNo);

    /**
     * 统计总数
     */
    @Select("SELECT COUNT(*) FROM t_student_info WHERE status = 1")
    int countTotal();

    /**
     * 统计已绑定数
     */
    @Select("SELECT COUNT(*) FROM t_student_info WHERE status = 1 AND is_bound = 1")
    int countBound();
    
    /**
     * Clear every denormalised student field by the authoritative bound user id.
     * t_user.student_no is encrypted and cannot be matched against the plaintext
     * value stored in t_student_info.
     */
    @Update("UPDATE t_user SET student_no = NULL, student_no_suffix = NULL, name = NULL, " +
            "class_id = NULL, class_name = NULL, grade = NULL, college = NULL, major = NULL, enroll_year = NULL " +
            "WHERE id = #{userId}")
    int clearUserBinding(@Param("userId") Long userId);
}
