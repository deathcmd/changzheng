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
     * 清除用户表的学号绑定（解绑时使用）
     */
    @Update("UPDATE t_user SET student_no = NULL WHERE student_no = #{studentNo}")
    int clearUserStudentNo(@Param("studentNo") String studentNo);
}
