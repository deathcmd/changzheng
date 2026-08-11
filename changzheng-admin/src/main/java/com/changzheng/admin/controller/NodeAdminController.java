package com.changzheng.admin.controller;

import com.changzheng.admin.dto.NodeContentRequest;
import com.changzheng.admin.dto.RouteNodeRequest;
import com.changzheng.admin.service.NodeAdminService;
import com.changzheng.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/nodes")
@RequiredArgsConstructor
public class NodeAdminController {

    private final NodeAdminService nodeAdminService;

    @GetMapping
    public R<List<Map<String, Object>>> listNodes() {
        return R.ok(nodeAdminService.listNodes());
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getNode(@PathVariable Long id) {
        return R.ok(nodeAdminService.getNode(id));
    }

    @PostMapping
    public R<Map<String, Object>> createNode(@Valid @RequestBody RouteNodeRequest request) {
        return R.ok("创建成功", nodeAdminService.createNode(request));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> updateNode(@PathVariable Long id,
                                              @Valid @RequestBody RouteNodeRequest request) {
        return R.ok("更新成功", nodeAdminService.updateNode(id, request));
    }

    @DeleteMapping("/{id}")
    public R<Void> disableNode(@PathVariable Long id) {
        nodeAdminService.disableNode(id);
        return R.ok("节点已禁用", null);
    }

    @GetMapping("/{nodeId}/contents")
    public R<List<Map<String, Object>>> listContents(@PathVariable Long nodeId) {
        return R.ok(nodeAdminService.listContents(nodeId));
    }

    @PostMapping("/{nodeId}/contents")
    public R<Map<String, Object>> saveContent(@RequestHeader("X-Admin-Id") Long adminId,
                                               @PathVariable Long nodeId,
                                               @Valid @RequestBody NodeContentRequest request) {
        return R.ok("保存成功", nodeAdminService.saveContent(nodeId, adminId, request));
    }

    @DeleteMapping("/{nodeId}/contents/{contentId}")
    public R<Void> disableContent(@PathVariable Long nodeId, @PathVariable Long contentId) {
        nodeAdminService.disableContent(nodeId, contentId);
        return R.ok("内容已删除", null);
    }
}
