// pages/mine/mine.js
const app = getApp()
const api = require('../../utils/api')
const config = require('../../config/index')

Page({
  data: {
    // 用户信息
    userInfo: null,
    isLogin: false,

    // 运动数据概览
    totalMileage: 0,
    totalSteps: 0,
    totalDays: 0,

    // 成就数量
    achievementCount: 0
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    this.checkLogin()
    this.syncFromGlobalData()
    if (app.globalData.isLogin) {
      this.loadUserData()
    }
  },

  // 检查登录状态
  checkLogin() {
    // 从Storage重新加载最新的用户信息
    const storedUserInfo = wx.getStorageSync('userInfo')
    if (storedUserInfo) {
      app.globalData.userInfo = storedUserInfo
    }

    const isLogin = app.globalData.isLogin
    const userInfo = app.globalData.userInfo

    this.setData({
      isLogin,
      userInfo: isLogin ? userInfo : null
    })
  },

  // 从全局数据同步
  syncFromGlobalData() {
    const { totalMileage, todaySteps } = app.globalData
    this.setData({
      totalMileage: totalMileage || 0,
      totalSteps: (totalMileage || 0) * 2000 // 估算总步数
    })
  },

  // 加载用户数据
  async loadUserData() {
    try {
      const overviewRes = await api.getMileageOverview()

      if (overviewRes.code === 200 && overviewRes.data) {
        this.setData({
          totalMileage: overviewRes.data.totalMileage || app.globalData.totalMileage || 0,
          totalSteps: overviewRes.data.totalSteps || 0,
          totalDays: overviewRes.data.totalDays || 0,
          achievementCount: overviewRes.data.unlockedNodeCount || 0
        })
      }
    } catch (e) {
      console.error('加载用户数据失败', e)
      // 使用全局数据
      this.syncFromGlobalData()
    }
  },

  // 去登录
  goLogin() {
    wx.navigateTo({
      url: '/pages/authorize/authorize'
    })
  },

  // 点击头像
  onAvatarTap() {
    if (!this.data.isLogin) {
      this.goLogin()
      return
    }
    // 未认证则跳转到认证页面
    if (!this.data.userInfo?.studentNo) {
      wx.navigateTo({
        url: '/pages/bind/bind'
      })
      return
    }
    // 已认证则显示个人信息
    const info = this.data.userInfo
    wx.showModal({
      title: '个人信息',
      content: `姓名：${info.name || info.nickName}
学号：${info.studentNo}
专业：${info.major || '-'}
班级：${info.className || '-'}
年级：${info.grade || '-'}`,
      showCancel: false,
      confirmText: '确定'
    })
  },

  // 跳转到运动记录
  goToHistory() {
    if (!this.data.isLogin) {
      this.goLogin()
      return
    }
    wx.navigateTo({
      url: '/pages/history/history'
    })
  },

  // 跳转到成就页面
  goToAchievement() {
    if (!this.data.isLogin) {
      this.goLogin()
      return
    }
    wx.navigateTo({
      url: '/pages/achievement/achievement'
    })
  },

  // 跳转到解锁节点
  goToNodes() {
    wx.switchTab({
      url: '/pages/map/map'
    })
  },

  // 跳转到绑定学号
  goToBind() {
    if (!this.data.isLogin) {
      this.goLogin()
      return
    }
    wx.navigateTo({
      url: '/pages/bind/bind'
    })
  },

  // 跳转到设置
  goToSetting() {
    // 使用全局状态判断登录
    if (!app.globalData.isLogin) {
      this.goLogin()
      return
    }

    wx.showActionSheet({
      itemList: ['更改头像或昵称', '清除缓存', '隐私设置', '关于我们'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.goToEditProfile()
        } else if (res.tapIndex === 1) {
          wx.showModal({
            title: '清除缓存',
            content: '确定要清除本地缓存吗？',
            success: (r) => {
              if (r.confirm) {
                wx.clearStorageSync()
                wx.showToast({ title: '清除成功', icon: 'success' })
              }
            }
          })
        } else if (res.tapIndex === 3) {
          this.goToAbout()
        }
      }
    })
  },

  // 跳转到编辑个人资料
  goToEditProfile() {
    wx.navigateTo({
      url: '/pages/profile/profile'
    })
  },

  // 跳转到关于长征
  goToAbout() {
    wx.showModal({
      title: '关于本应用',
      content: '「云上重走长征路」\n\n四川工商职业技术学院\n智能制造与信息工程学院\n\n红军长征（1934年-1936年），总里程约二万五千里，是人类历史上的伟大奇迹。\n\n本应用旨在通过步数换算里程，让同学们在运动中传承长征精神。',
      showCancel: false,
      confirmText: '知道了'
    })
  },

  // 退出登录
  onLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.logout()
          this.setData({
            isLogin: false,
            userInfo: null,  // 清除用户信息
            totalMileage: 0,
            totalSteps: 0,
            totalDays: 0,
            achievementCount: 0
          })
          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          })
        }
      }
    })
  },

  // 分享
  onShareAppMessage() {
    return {
      title: '云上重走长征路 - 用脚步丈量长征精神',
      path: '/pages/index/index'
    }
  }
})
