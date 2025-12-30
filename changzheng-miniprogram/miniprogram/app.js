// app.js
const config = require('./config/index')
const api = require('./utils/api')
const mock = require('./utils/mock')

App({
  globalData: {
    userInfo: null,
    token: null,
    isLogin: false,
    openId: null,
    // 系统配置
    stepToKmRate: 2000, // 默认2000步=1公里
    baseUrl: config.baseUrl,
    // 用户运动数据
    totalMileage: 0,
    todaySteps: 0,
    currentNode: null
  },

  onLaunch() {
    console.log('App onLaunch')
    // 检查登录态
    this.checkLoginStatus()
    // 获取系统配置
    this.loadSystemConfig()
    // 自动同步步数
    this.autoSyncSteps()
  },

  // 检查登录状态
  checkLoginStatus() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
      this.globalData.isLogin = true
      console.log('已登录，token:', token)
    } else {
      console.log('未登录')
    }
  },

  // 加载系统配置
  async loadSystemConfig() {
    try {
      const res = await api.getSystemConfig()
      if (res.code === 200 && res.data) {
        // 更新步数换算比例
        if (res.data.stepToKmRate) {
          this.globalData.stepToKmRate = res.data.stepToKmRate
        }
      }
    } catch (e) {
      console.error('加载系统配置失败', e)
    }
  },

  // 微信登录
  async wxLogin() {
    return new Promise((resolve, reject) => {
      // 调试日志
      console.log('[wxLogin] config.useMock =', config.useMock)
      console.log('[wxLogin] config.baseUrl =', config.baseUrl)
      console.log('[wxLogin] config.env =', config.env)

      // Mock模式直接返回模拟数据
      if (config.useMock) {
        // 添加延迟模拟网络请求
        setTimeout(() => {
          // 获取之前保存的用户信息（包含头像昵称等）
          const storedUserInfo = wx.getStorageSync('userInfo') || {}

          const mockData = {
            token: 'mock_token_' + Date.now(),
            userInfo: {
              ...mock.mockUserInfo,
              // 保留之前设置的头像、昵称、学生信息
              avatarUrl: storedUserInfo.avatarUrl || mock.mockUserInfo.avatarUrl,
              nickName: storedUserInfo.nickName || mock.mockUserInfo.nickName,
              studentNo: storedUserInfo.studentNo,
              studentId: storedUserInfo.studentId,
              name: storedUserInfo.name,
              major: storedUserInfo.major,
              className: storedUserInfo.className,
              grade: storedUserInfo.grade,
              college: storedUserInfo.college
            },
            openId: storedUserInfo.openId || 'mock_openid_' + Date.now()
          }
          this.globalData.token = mockData.token
          this.globalData.userInfo = mockData.userInfo
          this.globalData.openId = mockData.openId
          this.globalData.isLogin = true
          this.globalData.todaySteps = 8562
          this.globalData.totalMileage = 856

          wx.setStorageSync('token', mockData.token)
          wx.setStorageSync('userInfo', mockData.userInfo)
          wx.setStorageSync('openId', mockData.openId)

          console.log('[Mock] 登录成功', mockData)
          resolve(mockData)
        }, 500)
        return
      }

      wx.login({
        success: async (res) => {
          if (res.code) {
            try {
              // 获取之前保存的用户信息（用于保留头像昵称）
              const storedUserInfo = wx.getStorageSync('userInfo') || {}

              // 调用后端登录接口
              const loginRes = await api.login({ code: res.code })
              if (loginRes.code === 200) {
                // 后端返回的是 accessToken，需要正确解构
                const { accessToken, refreshToken, userInfo, needBind } = loginRes.data
                const token = accessToken

                // 合并用户信息：保留本地已设置的头像和昵称
                const mergedUserInfo = {
                  ...userInfo,
                  // 确保 userId 被保存
                  userId: userInfo && userInfo.userId,
                  // 如果本地有头像昵称，优先使用本地的
                  avatarUrl: storedUserInfo.avatarUrl || (userInfo && userInfo.avatarUrl),
                  nickName: storedUserInfo.nickName || (userInfo && userInfo.nickname)
                }

                console.log('[wxLogin] userId:', userInfo && userInfo.userId)
                console.log('[wxLogin] mergedUserInfo:', mergedUserInfo)

                // 保存登录信息
                this.globalData.token = token
                this.globalData.refreshToken = refreshToken
                this.globalData.userInfo = mergedUserInfo
                this.globalData.openId = userInfo && userInfo.userId
                this.globalData.isLogin = true
                this.globalData.needBind = needBind
                // 持久化
                wx.setStorageSync('token', token)
                wx.setStorageSync('refreshToken', refreshToken)
                wx.setStorageSync('userInfo', mergedUserInfo)
                resolve({ ...loginRes.data, token, userInfo: mergedUserInfo })
              } else {
                reject(new Error(loginRes.message || loginRes.msg || '登录失败'))
              }
            } catch (e) {
              reject(e)
            }
          } else {
            reject(new Error('获取code失败'))
          }
        },
        fail: reject
      })
    })
  },

  // 获取微信运动步数
  async getWeRunData() {
    return new Promise((resolve, reject) => {
      wx.getWeRunData({
        success: (res) => {
          resolve(res)
        },
        fail: (err) => {
          if (err.errMsg.includes('auth deny')) {
            // 引导用户授权
            wx.showModal({
              title: '授权提示',
              content: '需要获取您的微信运动数据来计算长征里程',
              confirmText: '去授权',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  wx.openSetting()
                }
              }
            })
          }
          reject(err)
        }
      })
    })
  },

  // 同步步数到服务器
  async syncSteps() {
    if (!this.globalData.isLogin) {
      console.log('未登录，跳过步数同步')
      return null
    }

    // Mock模式直接返回模拟数据
    if (config.useMock) {
      // 模拟步数数据（随机增加一些步数）
      const randomSteps = Math.floor(Math.random() * 2000) + 500
      const currentSteps = (this.globalData.todaySteps || 0) + randomSteps
      const newMileage = (this.globalData.totalMileage || 0) + (randomSteps / 2000)

      const mockSyncResult = {
        todaySteps: currentSteps,
        totalMileage: Math.round(newMileage * 100) / 100,
        todayMileage: Math.round((currentSteps / 2000) * 100) / 100,
        unlockedNodes: []
      }

      // 更新全局数据
      this.globalData.totalMileage = mockSyncResult.totalMileage
      this.globalData.todaySteps = mockSyncResult.todaySteps

      console.log('[Mock] 同步步数成功', mockSyncResult)
      return mockSyncResult
    }

    try {
      const weRunRes = await this.getWeRunData()
      const { encryptedData, iv } = weRunRes
      // 调用后端解密并同步
      const syncRes = await api.syncSteps({
        encryptedData,
        iv
      })
      if (syncRes.code === 200) {
        // 更新全局数据
        if (syncRes.data) {
          this.globalData.totalMileage = syncRes.data.totalMileage || 0
          this.globalData.todaySteps = syncRes.data.todaySteps || 0
          if (syncRes.data.unlockedNodes && syncRes.data.unlockedNodes.length > 0) {
            // 有新解锁的节点，弹出提示
            this.showNodeUnlockNotify(syncRes.data.unlockedNodes)
          }
        }
        return syncRes.data
      }
      return null
    } catch (e) {
      console.error('同步步数失败', e)
      return null
    }
  },

  // 显示节点解锁通知
  showNodeUnlockNotify(nodes) {
    if (!nodes || nodes.length === 0) return
    const node = nodes[0] // 显示第一个解锁的节点
    wx.showModal({
      title: '恭喜到达新节点！',
      content: `您已到达【${node.nodeName}】\n点击查看学习内容`,
      confirmText: '立即学习',
      cancelText: '稍后再看',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({
            url: `/pages/node/node?nodeId=${node.nodeId}`
          })
        }
      }
    })
  },

  // 登出（完全清除本地信息，认证状态保存在服务器不受影响）
  logout() {
    this.globalData.token = null
    this.globalData.userInfo = null
    this.globalData.openId = null
    this.globalData.isLogin = false
    this.globalData.totalMileage = 0
    this.globalData.todaySteps = 0
    this.globalData.currentNode = null
    // 清除所有本地存储
    wx.removeStorageSync('token')
    wx.removeStorageSync('refreshToken')
    wx.removeStorageSync('userInfo')
    wx.removeStorageSync('openId')
    wx.removeStorageSync('lastSyncTime')
  },

  // 自动同步步数
  async autoSyncSteps() {
    // 等待登录状态确认
    if (!this.globalData.isLogin) {
      console.log('未登录，跳过自动同步')
      return
    }

    // 检查上次同步时间，避免频繁同步
    const lastSyncTime = wx.getStorageSync('lastSyncTime') || 0
    const now = Date.now()
    const syncInterval = 5 * 60 * 1000 // 5分钟同步一次

    if (now - lastSyncTime < syncInterval) {
      console.log('同步间隔未到，跳过同步')
      return
    }

    console.log('开始自动同步步数...')
    const result = await this.syncSteps()
    if (result) {
      wx.setStorageSync('lastSyncTime', now)
      console.log('自动同步完成', result)
    }
  },

  // 启动定时同步（小程序前台运行时）
  startSyncTimer() {
    // 清除旧定时器
    if (this.syncTimer) {
      clearInterval(this.syncTimer)
    }

    // 每5分钟同步一次
    this.syncTimer = setInterval(() => {
      this.autoSyncSteps()
    }, 5 * 60 * 1000)
  },

  // 停止定时同步
  stopSyncTimer() {
    if (this.syncTimer) {
      clearInterval(this.syncTimer)
      this.syncTimer = null
    }
  }
})
