package com.changzheng.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NodeContentRequest {

    private Long id;

    @NotBlank(message = "内容类型不能为空")
    @Pattern(regexp = "^(video|audio|article|image)$", message = "内容类型不受支持")
    private String contentType;

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题不能超过128个字符")
    private String title;

    @Size(max = 32, message = "时长或字数说明不能超过32个字符")
    private String duration;

    @Size(max = 512, message = "媒体地址不能超过512个字符")
    @Pattern(regexp = "^(|https://[^\s]+|/(?!/)[^\s]+)$", message = "媒体地址必须是HTTPS地址或站内路径")
    private String mediaUrl;

    @Size(max = 20000, message = "文章内容不能超过20000个字符")
    private String content;

    @Size(max = 512, message = "封面地址不能超过512个字符")
    @Pattern(regexp = "^(|https://[^\s]+|/(?!/)[^\s]+)$", message = "封面地址必须是HTTPS地址或站内路径")
    private String coverUrl;

    @NotNull(message = "排序序号不能为空")
    @Min(value = 0, message = "排序序号不能小于0")
    @Max(value = 10000, message = "排序序号超出范围")
    private Integer sortOrder;

    @NotNull(message = "自动播放状态不能为空")
    private Boolean autoPlay;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
