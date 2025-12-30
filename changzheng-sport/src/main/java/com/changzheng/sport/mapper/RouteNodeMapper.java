package com.changzheng.sport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changzheng.common.entity.RouteNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 路线节点Mapper
 */
@Mapper
public interface RouteNodeMapper extends BaseMapper<RouteNode> {

    /**
     * 获取当前所在节点(已到达的最后一个节点)
     */
    @Select("SELECT * FROM t_route_node WHERE status = 1 AND mileage_threshold <= #{mileage} ORDER BY mileage_threshold DESC LIMIT 1")
    RouteNode selectCurrentNode(@Param("mileage") BigDecimal mileage);

    /**
     * 获取下一个节点(未到达的第一个节点)
     */
    @Select("SELECT * FROM t_route_node WHERE status = 1 AND mileage_threshold > #{mileage} ORDER BY mileage_threshold ASC LIMIT 1")
    RouteNode selectNextNode(@Param("mileage") BigDecimal mileage);
}
