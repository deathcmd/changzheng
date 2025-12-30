package com.changzheng.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changzheng.admin.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员 Mapper
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
