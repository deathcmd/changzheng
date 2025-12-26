package com.changzheng.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changzheng.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE openid = #{openid} LIMIT 1")
    User selectByOpenid(@Param("openid") String openid);

    @Select("SELECT * FROM t_user WHERE student_no = #{studentNo} LIMIT 1")
    User selectByStudentNo(@Param("studentNo") String studentNo);

    @Select("SELECT COUNT(*) FROM t_user WHERE student_no = #{studentNo}")
    int countByStudentNo(@Param("studentNo") String studentNo);
}
