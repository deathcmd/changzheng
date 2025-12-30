package com.changzheng.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changzheng.common.entity.StudentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 学生信息Mapper
 */
@Mapper
public interface StudentInfoMapper extends BaseMapper<StudentInfo> {

    /**
     * 根据学号和姓名查询学生
     */
    @Select("SELECT * FROM t_student_info WHERE student_no = #{studentNo} AND name = #{name} AND status = 1")
    StudentInfo selectByStudentNoAndName(@Param("studentNo") String studentNo, @Param("name") String name);

    /**
     * 根据学号查询学生
     */
    @Select("SELECT * FROM t_student_info WHERE student_no = #{studentNo} AND status = 1")
    StudentInfo selectByStudentNo(@Param("studentNo") String studentNo);

    /**
     * 更新绑定状态
     */
    @Update("UPDATE t_student_info SET is_bound = 1, bound_user_id = #{userId}, bound_at = NOW() WHERE id = #{id}")
    int updateBoundStatus(@Param("id") Long id, @Param("userId") Long userId);
}
