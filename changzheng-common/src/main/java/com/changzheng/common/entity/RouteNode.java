package com.changzheng.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 路线节点实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_route_node")
public class RouteNode extends BaseEntity {

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 解锁所需里程(公里)
     */
    private BigDecimal mileageThreshold;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 节点简介
     */
    private String description;

    /**
     * 节点图标URL
     */
    private String iconUrl;

    /**
     * 状态: 0-禁用 1-启用
     */
    private Integer status;
}
