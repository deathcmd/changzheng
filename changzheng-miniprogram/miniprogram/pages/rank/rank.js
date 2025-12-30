// pages/rank/rank.js
const app = getApp()
const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    // 当前选中的tab: total(总榜) / grade(年级榜)
    activeTab: 'total',
    // 排行数据
    rankList: [],
    // 用户年级
    userGrade: '',
    // 我的排名
    myRank: null,
    // 加载状态
    loading: true,
    // 分页
    page: 1,
    pageSize: 20,
    hasMore: true,
    // 登录状态
    isLogin: false
  },

  onLoad() {
    this.setData({ isLogin: app.globalData.isLogin })
    this.loadRankData()
  },

  onShow() {
    // 更新登录状态
    this.setData({ isLogin: app.globalData.isLogin })
    // 刷新我的排名
    if (app.globalData.isLogin) {
      this.loadMyRank()
    }
  },

  // 下拉刷新
  async onPullDownRefresh() {
    this.setData({ page: 1, hasMore: true })
    await this.loadRankData()
    wx.stopPullDownRefresh()
  },

  // 上拉加载
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMore()
    }
  },

  // 加载排行数据
  async loadRankData() {
    this.setData({ loading: true })
    
    try {
      const { page, pageSize, activeTab } = this.data
      const params = { page, pageSize }
      
      // 根据tab加载不同数据
      const res = activeTab === 'total' 
        ? await api.getTotalRank(params)
        : await api.getGradeRank(params)
      
      if (res.code === 200 && res.data) {
        const list = res.data.records || res.data.list || []
        const total = res.data.total || 0
        const userGrade = res.data.userGrade || ''
        
        this.setData({
          rankList: page === 1 ? list : [...this.data.rankList, ...list],
          hasMore: this.data.rankList.length + list.length < total,
          userGrade: userGrade
        })
      }
      
      // 加载我的排名
      if (app.globalData.isLogin) {
        await this.loadMyRank()
      }
      
    } catch (e) {
      console.error('加载排行榜失败', e)
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 加载更多
  async loadMore() {
    this.setData({ page: this.data.page + 1 })
    await this.loadRankData()
  },

  // 加载我的排名
  async loadMyRank() {
    try {
      const res = await api.getMyRank()
      if (res.code === 200 && res.data) {
        this.setData({ myRank: res.data })
      }
    } catch (e) {
      console.error('加载我的排名失败', e)
    }
  },

  // 切换Tab
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (tab !== this.data.activeTab) {
      this.setData({
        activeTab: tab,
        page: 1,
        rankList: [],
        hasMore: true
      })
      this.loadRankData()
    }
  },

  // 查看用户详情
  onUserTap(e) {
    const { item } = e.currentTarget.dataset
    // 可以跳转到用户主页或查看详情
    console.log('查看用户', item)
  },

  // 去登录
  goLogin() {
    wx.navigateTo({
      url: '/pages/authorize/authorize'
    })
  },

  // 获取排名样式
  getRankClass(rank) {
    if (rank === 1) return 'rank-gold'
    if (rank === 2) return 'rank-silver'
    if (rank === 3) return 'rank-bronze'
    return ''
  }
})
