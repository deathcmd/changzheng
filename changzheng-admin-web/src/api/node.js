import request from '@/utils/request'

// 获取节点列表
export function getNodeList(params) {
  return request({
    url: '/admin/nodes',
    method: 'get',
    params
  })
}

// 获取节点详情
export function getNodeDetail(id) {
  return request({
    url: `/admin/nodes/${id}`,
    method: 'get'
  })
}

// 创建节点
export function createNode(data) {
  return request({
    url: '/admin/nodes',
    method: 'post',
    data
  })
}

// 更新节点
export function updateNode(id, data) {
  return request({
    url: `/admin/nodes/${id}`,
    method: 'put',
    data
  })
}

// 删除节点
export function deleteNode(id) {
  return request({
    url: `/admin/nodes/${id}`,
    method: 'delete'
  })
}

// 获取节点内容列表
export function getNodeContents(nodeId) {
  return request({
    url: `/admin/nodes/${nodeId}/contents`,
    method: 'get'
  })
}

// 保存节点内容
export function saveNodeContent(nodeId, data) {
  return request({
    url: `/admin/nodes/${nodeId}/contents`,
    method: 'post',
    data
  })
}
