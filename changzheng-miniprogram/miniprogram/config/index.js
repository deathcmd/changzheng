// config/index.js - 配置文件

// 环境配置
const ENV = {
  dev: {
    baseUrl: 'http://localhost:8080',
    debug: true,
    useMock: true  // 开发/测试环境使用 Mock 数据
  },
  prod: {
    // 部署时修改为你的服务器域名
    baseUrl: 'https://deathcmd.cn',
    debug: false,
    useMock: false
  }
}

// 当前环境 - 部署时改为 'prod'
const currentEnv = 'prod'

module.exports = {
  ...ENV[currentEnv],
  env: currentEnv,
  
  // 版本号
  version: '1.0.0',
  
  // 长征路线总里程(公里)
  totalDistance: 25000,
  
  // 默认步数换算比例 (步数/公里)
  defaultStepToKmRate: 2000,
  
  // 每日最大有效步数
  maxDailySteps: 30000,
  
  // 异常步数阈值
  abnormalStepsThreshold: 50000,
  
  // 排行榜类型
  rankTypes: {
    PERSONAL: 'personal',
    CLASS: 'class',
    GRADE: 'grade'
  },
  
  // 缓存key
  storageKeys: {
    TOKEN: 'token',
    USER_INFO: 'userInfo',
    OPEN_ID: 'openId',
    LAST_SYNC_TIME: 'lastSyncTime'
  }
}
