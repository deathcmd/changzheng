package com.changzheng.sport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changzheng.common.entity.UserNodeProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户节点进度Mapper
 */
@Mapper
public interface UserNodeProgressMapper extends BaseMapper<UserNodeProgress> {

    @Select("SELECT * FROM t_user_node_progress WHERE user_id = #{userId} AND node_id = #{nodeId} LIMIT 1")
    UserNodeProgress selectByUserAndNode(@Param("userId") Long userId, @Param("nodeId") Long nodeId);

    @Select("SELECT COUNT(*) FROM t_user_node_progress WHERE user_id = #{userId} AND unlock_status = 1")
    Long countUnlockedByUserId(@Param("userId") Long userId);
}
