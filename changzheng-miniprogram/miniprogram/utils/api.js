// utils/api.js - API接口封装
const request = require('./request')

// ==================== 认证模块 ====================
// 微信登录
const login = (data) => {
  return request.post('/auth/wx/login', data, { noAuth: true })
}

// 学生身份认证
const bindStudent = (data) => {
  return request.post('/auth/bindStudent', data)
}

// 获取用户信息
const getUserInfo = () => {
  return request.get('/auth/user/info')
}

// 更新用户资料(头像昵称)
const updateUserProfile = (data) => {
  return request.post('/auth/profile', data)
}

// ==================== 运动模块 ====================
// 同步步数
const syncSteps = (data) => {
  return request.post('/sport/steps/sync', data)
}

// 获取用户里程概览
const getMileageOverview = () => {
  return request.get('/sport/mileage/overview')
}

// 获取每日步数记录
const getDailySteps = (params) => {
  return request.get('/sport/steps/daily', params)
}

// 获取里程流水
const getMileageLedger = (params) => {
  return request.get('/sport/mileage/ledger', params)
}

// ==================== 路线节点模块 ====================
// 获取路线节点列表
const getRouteNodes = () => {
  return request.get('/content/route/nodes')
}

// 获取节点详情
const getNodeDetail = (nodeId) => {
  return request.get(`/content/node/${nodeId}`)
}

// 获取用户节点进度
const getUserNodeProgress = () => {
  return request.get('/content/user/progress')
}

// 获取节点学习内容
const getNodeContents = (nodeId) => {
  return request.get(`/content/node/${nodeId}/contents`)
}

// 获取内容详情
const getContentDetail = (contentId) => {
  return request.get(`/content/detail/${contentId}`)
}

// 标记内容已学习
const markContentLearned = (contentId) => {
  return request.post(`/content/learned/${contentId}`)
}

// ==================== 排行榜模块 ====================
// 获取个人排行榜
const getPersonalRank = (params) => {
  return request.get('/rank/personal', params)
}

// 获取班级排行榜
const getClassRank = (params) => {
  return request.get('/rank/class', params)
}

// 获取年级排行榜
const getGradeRank = (params) => {
  return request.get('/rank/grade', params)
}

// 获取我的排名
const getMyRank = (type) => {
  return request.get('/rank/my', { type })
}

// ==================== 成就模块 ====================
// 获取成就列表
const getAchievements = () => {
  return request.get('/rank/achievements')
}

// 获取用户成就
const getUserAchievements = () => {
  return request.get('/rank/user/achievements')
}

// ==================== 系统配置 ====================
// 获取系统配置
const getSystemConfig = () => {
  return request.get('/common/config', {}, { noAuth: true })
}

// 获取轮播图
const getBanners = () => {
  return request.get('/common/banners', {}, { noAuth: true })
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
  getPersonalRank,
  getClassRank,
  getGradeRank,
  getMyRank,
  // 成就
  getAchievements,
  getUserAchievements,
  // 系统
  getSystemConfig,
  getBanners
}
