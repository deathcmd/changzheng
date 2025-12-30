// utils/api.js - API接口封装
const request = require('./request')

// ==================== 认证模块 ====================
// 微信登录
const login = (data) => {
  return request.post('/api/auth/wx/login', data, { noAuth: true })
}

// 学生身份认证
const bindStudent = (data) => {
  return request.post('/api/auth/bindStudent', data)
}

// 获取用户信息
const getUserInfo = () => {
  return request.get('/api/auth/user/info')
}

// 更新用户资料(头像昵称)
const updateUserProfile = (data) => {
  return request.post('/api/auth/profile', data)
}

// ==================== 运动模块 ====================
// 同步步数
const syncSteps = (data) => {
  return request.post('/api/sport/syncSteps', data)
}

// 获取用户里程概览（使用 progress 接口）
const getMileageOverview = () => {
  return request.get('/api/sport/progress')
}

// 获取每日步数记录
const getDailySteps = (params) => {
  return request.get('/api/sport/dailySteps', params)
}

// 获取里程流水
const getMileageLedger = (params) => {
  return request.get('/api/sport/mileage/ledger', params)
}

// ==================== 路线节点模块 ====================
// 获取路线节点列表
const getRouteNodes = () => {
  return request.get('/api/content/route/nodes')
}

// 获取节点详情
const getNodeDetail = (nodeId) => {
  return request.get(`/api/content/node/${nodeId}`)
}

// 获取用户节点进度
// 注意：使用 sport 服务的 progress 接口
const getUserNodeProgress = () => {
  return request.get('/api/sport/progress')
}

// 获取节点学习内容
const getNodeContents = (nodeId) => {
  return request.get(`/api/content/node/${nodeId}/contents`)
}

// 获取内容详情
const getContentDetail = (contentId) => {
  return request.get(`/api/content/detail/${contentId}`)
}

// 标记内容已学习
const markContentLearned = (contentId) => {
  return request.post(`/api/content/learned/${contentId}`)
}

// ==================== 排行榜模块 ====================
// 获取总榜
const getTotalRank = (params) => {
  return request.get('/api/rank/total', params)
}

// 获取年级排行榜
const getGradeRank = (params) => {
  return request.get('/api/rank/grade', params)
}

// 获取我的排名
const getMyRank = () => {
  return request.get('/api/rank/my')
}

// ==================== 成就模块 ====================
// 获取成就列表
const getAchievements = () => {
  return request.get('/api/rank/achievements')
}

// 获取用户成就
const getUserAchievements = () => {
  return request.get('/api/rank/user/achievements')
}

// ==================== 系统配置 ====================
// 获取系统配置
const getSystemConfig = () => {
  return request.get('/api/common/config', {}, { noAuth: true })
}

// 获取轮播图
const getBanners = () => {
  return request.get('/api/common/banners', {}, { noAuth: true })
}

module.exports = {
  // 认证
  login,
  bindStudent,
  getUserInfo,
  updateUserProfile,
  // 运动
  syncSteps,
  getMileageOverview,
  getDailySteps,
  getMileageLedger,
  // 路线节点
  getRouteNodes,
  getNodeDetail,
  getUserNodeProgress,
  getNodeContents,
  getContentDetail,
  markContentLearned,
  // 排行榜
  getTotalRank,
  getGradeRank,
  getMyRank,
  // 成就
  getAchievements,
  getUserAchievements,
  // 系统
  getSystemConfig,
  getBanners
}
