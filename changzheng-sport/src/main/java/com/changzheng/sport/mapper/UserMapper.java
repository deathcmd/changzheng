package com.changzheng.sport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changzheng.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper(Sport服务)
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
