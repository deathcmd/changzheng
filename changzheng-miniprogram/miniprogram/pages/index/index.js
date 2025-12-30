// pages/index/index.js
const app = getApp()
const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    // 用户信息
    userInfo: null,
    isLogin: false,

    // 运动数据
    todaySteps: 0,
    totalMileage: 0,
    todayMileage: 0,

    // 当前节点
    currentNode: null,
    nextNode: null,
    progressPercent: 0,

    // 排名
    myRank: 0,

    // 轮播图
    banners: [],

    // 加载状态
    loading: true,
    syncing: false
  },

  onLoad() {
    this.checkLoginAndLoad()
  },

  onShow() {
    // 从Storage重新加载最新的用户信息
    const storedUserInfo = wx.getStorageSync('userInfo')
    if (storedUserInfo) {
      app.globalData.userInfo = storedUserInfo
    }

    // 更新登录状态
    this.setData({
      isLogin: app.globalData.isLogin,
      userInfo: app.globalData.userInfo
    })
    if (app.globalData.isLogin) {
      // 自动同步步数
      this.autoSyncAndLoad()
    }
  },

  // 自动同步并加载数据
  async autoSyncAndLoad() {
    // 先自动同步
    await app.autoSyncSteps()

    // 更新界面数据
    this.setData({
      todaySteps: app.globalData.todaySteps || 0,
      totalMileage: app.globalData.totalMileage || 0
    })

    // 加载其他页面数据
    await this.loadPageData()
  },

  // 下拉刷新
  async onPullDownRefresh() {
    await this.syncAndRefresh()
    wx.stopPullDownRefresh()
  },

  // 检查登录并加载数据
  async checkLoginAndLoad() {
    if (!app.globalData.isLogin) {
      this.setData({ loading: false })
      return
    }

    this.setData({
      isLogin: true,
      userInfo: app.globalData.userInfo
    })

    await this.loadPageData()
  },

  // 加载页面数据
  async loadPageData() {
    this.setData({ loading: true })

    try {
      // 并行加载数据
      const [overviewRes, progressRes, rankRes] = await Promise.all([
        api.getMileageOverview(),
        api.getUserNodeProgress(),
        api.getMyRank()
      ])

      // 处理里程概览
      if (overviewRes.code === 200 && overviewRes.data) {
        const { todaySteps, totalMileage, todayMileage } = overviewRes.data
        this.setData({
          todaySteps: todaySteps || 0,
          totalMileage: totalMileage || 0,
          todayMileage: todayMileage || 0
        })
        // 更新全局数据
        app.globalData.totalMileage = totalMileage || 0
        app.globalData.todaySteps = todaySteps || 0
      }

      // 处理节点进度
      if (progressRes.code === 200 && progressRes.data) {
        const { currentNode, nextNode, progressPercent } = progressRes.data
        this.setData({
          currentNode,
          nextNode,
          progressPercent: (progressPercent || 0).toFixed(2)
        })
      }

      // 处理排名
      if (rankRes.code === 200 && rankRes.data) {
        this.setData({ myRank: rankRes.data.rank || 0 })
      }

    } catch (e) {
      console.error('加载数据失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  // 同步步数并刷新（手动触发，强制刷新）
  async syncAndRefresh() {
    if (this.data.syncing) return

    this.setData({ syncing: true })

    try {
      // 清除上次同步时间，强制同步
      wx.removeStorageSync('lastSyncTime')

      const syncResult = await app.syncSteps()
      if (syncResult) {
        // 直接更新界面数据
        this.setData({
          todaySteps: syncResult.todaySteps || 0,
          totalMileage: syncResult.totalMileage || 0,
          todayMileage: syncResult.todayMileage || 0
        })

        // 记录同步时间
        wx.setStorageSync('lastSyncTime', Date.now())

        wx.showToast({
          title: '刷新成功',
          icon: 'success'
        })
      } else {
        wx.showToast({
          title: '刷新失败',
          icon: 'none'
        })
      }
    } catch (e) {
      console.error('同步失败', e)
      wx.showToast({
        title: '刷新失败',
        icon: 'none'
      })
    } finally {
      this.setData({ syncing: false })
    }
  },

  // 去登录
  goLogin() {
    wx.navigateTo({
      url: '/pages/authorize/authorize'
    })
  },

  // 点击同步按钮
  onSyncTap() {
    if (!this.data.isLogin) {
      this.goLogin()
      return
    }
    this.syncAndRefresh()
  },

  // 查看长征地图
  goMap() {
    wx.switchTab({
      url: '/pages/map/map'
    })
  },

  // 查看排行榜
  goRank() {
    wx.switchTab({
      url: '/pages/rank/rank'
    })
  },

  // 查看我的成就
  goAchievement() {
    wx.navigateTo({
      url: '/pages/achievement/achievement'
    })
  },

  // 查看运动记录
  goHistory() {
    wx.navigateTo({
      url: '/pages/history/history'
    })
  },

  // 查看当前节点
  goCurrentNode() {
    if (this.data.currentNode && this.data.currentNode.nodeId) {
      wx.navigateTo({
        url: `/pages/node/node?nodeId=${this.data.currentNode.nodeId}`
      })
    }
  },

  // 格式化步数显示
  formatSteps(steps) {
    return util.formatSteps(steps)
  },

  // 格式化里程显示
  formatMileage(km) {
    return util.formatMileage(km)
  }
})
