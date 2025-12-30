// utils/mock.js - Mock数据

// 模拟用户信息
const mockUserInfo = {
  id: 1,
  openId: 'mock_openid_123',
  nickName: '', // 默认为空，需要用户设置
  avatarUrl: '', // 默认为空，需要用户设置
  studentId: '',
  className: '',
  grade: ''
}

// 模拟节点数据（与地图坐标匹配）
const mockNodes = [
  { id: 1, nodeName: '瑞金', cumulativeKm: 0, description: '长征出发地，中央红军从这里开始了伟大的战略转移。' },
  { id: 2, nodeName: '于都', cumulativeKm: 60, description: '红军长征集结出发地，8.6万红军在此渡过于都河。' },
  { id: 3, nodeName: '信丰', cumulativeKm: 200, description: '红军突破第一道封锁线的重要战场。' },
  { id: 4, nodeName: '湘江', cumulativeKm: 400, description: '湘江战役是长征途中最惨烈的战役，红军付出了巨大牺牲。' },
  { id: 5, nodeName: '遂川', cumulativeKm: 800, description: '红军在此进行了重要的休整。' },
  { id: 6, nodeName: '遵义', cumulativeKm: 1200, description: '遵义会议确立了毛泽东的领导地位，是党历史上的转折点。' },
  { id: 7, nodeName: '赤水河', cumulativeKm: 1500, description: '四渡赤水是毛泽东军事指挥的得意之笔。' },
  { id: 8, nodeName: '金沙江', cumulativeKm: 2500, description: '巧渡金沙江，红军摆脱了敌人的围追堵截。' },
  { id: 9, nodeName: '大渡河', cumulativeKm: 3200, description: '红军强渡大渡河，显示了英勇无畏的革命精神。' },
  { id: 10, nodeName: '泸定桥', cumulativeKm: 3500, description: '飞夺泸定桥，22名勇士冒着枪林弹雨爬过铁索桥。' },
  { id: 11, nodeName: '夹金山', cumulativeKm: 4000, description: '翠越夹金山，红军战胜了恶劣的自然环境。' },
  { id: 12, nodeName: '毛儿盖', cumulativeKm: 4500, description: '两河口会议召开地，决定了红军北上的战略方向。' },
  { id: 13, nodeName: '草地', cumulativeKm: 5000, description: '过草地是长征中最艰难的行程之一。' },
  { id: 14, nodeName: '腊子口', cumulativeKm: 6000, description: '突破腊子口，打开了北上的通道。' },
  { id: 15, nodeName: '哈达铺', cumulativeKm: 7000, description: '红军在此进行了重要的休整和补给。' },
  { id: 16, nodeName: '吴起镇', cumulativeKm: 8000, description: '中央红军到达陕北，与陕北红军会师。' },
  { id: 17, nodeName: '会宁', cumulativeKm: 25000, description: '红军三大主力在会宁胜利会师，长征胜利结束。' }
]

// 模拟成就数据
const mockAchievements = [
  { id: 1, name: '初出茅庐', description: '完成首次步数同步', iconUrl: '/images/achievement-1.png', isUnlocked: true, unlockTime: '2024-12-20' },
  { id: 2, name: '日行千里', description: '单日步数超过10000步', iconUrl: '/images/achievement-2.png', isUnlocked: true, unlockTime: '2024-12-21' },
  { id: 3, name: '首战告捷', description: '解锁第一个长征节点', iconUrl: '/images/achievement-3.png', isUnlocked: true, unlockTime: '2024-12-22' },
  { id: 4, name: '坚持不懈', description: '连续7天完成运动', iconUrl: '/images/achievement-4.png', isUnlocked: false, condition: '连续运动7天' },
  { id: 5, name: '长征先锋', description: '累计里程达到1000公里', iconUrl: '/images/achievement-5.png', isUnlocked: false, condition: '累计1000公里' },
  { id: 6, name: '红军战士', description: '完成全部长征路程', iconUrl: '/images/achievement-6.png', isUnlocked: false, condition: '完成25000公里' }
]

// 模拟排行榜数据（使用学生认证信息）
const mockPersonalRank = [
  { id: 1, realName: '张*明', avatarUrl: '/images/default-avatar.png', totalMileage: 1580, major: '软件技术', className: '软件2301班', grade: '2023级' },
  { id: 2, realName: '李*', avatarUrl: '/images/default-avatar.png', totalMileage: 1420, major: '计算机应用技术', className: '计应2302班', grade: '2023级' },
  { id: 3, realName: '王*华', avatarUrl: '/images/default-avatar.png', totalMileage: 1350, major: '人工智能技术应用', className: '人工智能2401班', grade: '2024级' },
  { id: 4, realName: '赵*龙', avatarUrl: '/images/default-avatar.png', totalMileage: 1280, major: '大数据技术', className: '大数据2301班', grade: '2023级' },
  { id: 5, realName: '刘*', avatarUrl: '/images/default-avatar.png', totalMileage: 1150, major: '物联网应用技术', className: '物联网2302班', grade: '2023级' },
  { id: 6, realName: '陈*敏', avatarUrl: '/images/default-avatar.png', totalMileage: 980, major: '软件技术', className: '软件2401班', grade: '2024级' },
  { id: 7, realName: '杨*', avatarUrl: '/images/default-avatar.png', totalMileage: 856, major: '计算机应用技术', className: '计应2401班', grade: '2024级' }
]

// Mock API响应
const mockResponses = {
  // 登录
  'POST:/auth/wx-login': {
    code: 200,
    data: {
      token: 'mock_token_' + Date.now(),
      userInfo: mockUserInfo,
      openId: 'mock_openid_123'
    }
  },
  
  // 系统配置
  'GET:/system/config': {
    code: 200,
    data: { stepToKmRate: 2000 }
  },
  
  // 里程概览
  'GET:/sport/mileage/overview': {
    code: 200,
    data: {
      todaySteps: 8562,
      totalSteps: 258600,
      todayMileage: 4.28,
      totalMileage: 856,
      totalDays: 30
    }
  },
  
  // 同步步数
  'POST:/sport/steps/sync': {
    code: 200,
    data: {
      todaySteps: 8562,
      totalMileage: 860,
      unlockedNodes: []
    }
  },
  
  // 获取节点列表
  'GET:/content/nodes': {
    code: 200,
    data: mockNodes
  },
  
  // 用户节点进度
  'GET:/sport/progress': {
    code: 200,
    data: {
      currentMileage: 856,
      unlockedNodes: mockNodes.slice(0, 5).map(n => ({ nodeId: n.id, nodeName: n.nodeName })),
      currentNode: mockNodes[4],
      nextNode: { ...mockNodes[5], remainingKm: 344 },
      progressPercent: 3.4
    }
  },
  
  // 节点详情
  'GET:/content/node/detail': {
    code: 200,
    data: mockNodes[2]
  },
  
  // 节点内容
  'GET:/content/node/contents': {
    code: 200,
    data: [
      { id: 1, title: '湘江战役的历史背景', contentType: 'article', duration: '5分钟', isLearned: true },
      { id: 2, title: '湘江战役纪录片', contentType: 'video', duration: '15分钟', isLearned: true },
      { id: 3, title: '红军英烈的故事', contentType: 'audio', duration: '8分钟', isLearned: false },
      { id: 4, title: '知识问答', contentType: 'quiz', duration: '10题', isLearned: false }
    ]
  },
  
  // 个人排行
  'GET:/rank/personal': {
    code: 200,
    data: {
      records: mockPersonalRank,
      total: mockPersonalRank.length
    }
  },
  
  // 班级排行
  'GET:/rank/class': {
    code: 200,
    data: {
      records: [
        { id: 1, name: '体育2301班', mileage: 15800 },
        { id: 2, name: '计算机2302班', mileage: 14200 },
        { id: 3, name: '电子2401班', mileage: 13500 }
      ],
      total: 3
    }
  },
  
  // 年级排行
  'GET:/rank/grade': {
    code: 200,
    data: {
      records: [
        { id: 1, name: '2023级', mileage: 158000 },
        { id: 2, name: '2024级', mileage: 142000 },
        { id: 3, name: '2022级', mileage: 135000 }
      ],
      total: 3
    }
  },
  
  // 我的排名
  'GET:/rank/my': {
    code: 200,
    data: {
      rank: 7,
      totalMileage: 856,
      realName: '我*名',
      avatarUrl: '/images/default-avatar.png',
      major: '软件技术',
      className: '软件2301班'
    }
  },
  
  // 成就列表
  'GET:/user/achievements': {
    code: 200,
    data: mockAchievements
  }
}

// 获取Mock响应
function getMockResponse(method, url) {
  // 移除查询参数
  const path = url.split('?')[0]
  const key = `${method}:${path}`
  
  // 精确匹配
  if (mockResponses[key]) {
    return mockResponses[key]
  }
  
  // 模糊匹配
  for (const k in mockResponses) {
    if (key.includes(k.split(':')[1])) {
      return mockResponses[k]
    }
  }
  
  // 默认响应
  return { code: 200, data: null, msg: 'Mock: No data' }
}

module.exports = {
  getMockResponse,
  mockUserInfo,
  mockNodes,
  mockAchievements
}
