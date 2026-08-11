package com.changzheng.admin.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RouteNodeRequest {

    @NotBlank(message = "节点编码不能为空")
    @Pattern(regexp = "^[A-Z0-9_-]{2,32}$", message = "节点编码只能包含大写字母、数字、下划线和连字符")
    private String nodeCode;

    @NotBlank(message = "节点名称不能为空")
    @Size(max = 64, message = "节点名称不能超过64个字符")
    private String nodeName;

    @NotNull(message = "里程阈值不能为空")
    @DecimalMin(value = "0.00", message = "里程阈值不能小于0")
    @DecimalMax(value = "99999999.99", message = "里程阈值超出范围")
    @Digits(integer = 8, fraction = 2, message = "里程阈值最多保留两位小数")
    private BigDecimal mileageThreshold;

    @NotNull(message = "排序序号不能为空")
    @Min(value = 0, message = "排序序号不能小于0")
    @Max(value = 10000, message = "排序序号超出范围")
    private Integer sortOrder;

    @DecimalMin(value = "-180.000000", message = "经度不能小于-180")
    @DecimalMax(value = "180.000000", message = "经度不能大于180")
    @Digits(integer = 3, fraction = 6, message = "经度最多保留六位小数")
    private BigDecimal longitude;

    @DecimalMin(value = "-90.000000", message = "纬度不能小于-90")
    @DecimalMax(value = "90.000000", message = "纬度不能大于90")
    @Digits(integer = 2, fraction = 6, message = "纬度最多保留六位小数")
    private BigDecimal latitude;

    @Size(max = 512, message = "节点描述不能超过512个字符")
    private String description;

    @Size(max = 512, message = "图标地址不能超过512个字符")
    @Pattern(regexp = "^(|https://[^\s]+|/(?!/)[^\s]+)$", message = "图标地址必须是HTTPS地址或站内路径")
    private String iconUrl;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;
}
