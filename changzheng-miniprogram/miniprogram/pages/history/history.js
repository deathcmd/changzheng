// pages/history/history.js
const app = getApp()
const api = require('../../utils/api')

// 格式化日期
function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 获取日期文本
function getDateText(date, today) {
  const diff = Math.floor((today - date) / (1000 * 60 * 60 * 24))
  if (diff === 0) return '今天'
  if (diff === 1) return '昨天'
  if (diff === 2) return '前天'
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// 获取星期
function getWeekDay(date) {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[date.getDay()]
}

Page({
  data: {
    isLogin: false,
    records: [],
    loading: true,
    
    // 统计数据
    totalSteps: 0,
    totalMileage: 0,
    totalDays: 0,
    avgSteps: 0,
    
    // 当前选中的月份
    currentMonth: '',
    
    // 步数换算比例
    stepToKmRate: 2000
  },

  onLoad() {
    this.setData({
      isLogin: app.globalData.isLogin,
      currentMonth: this.getCurrentMonth()
    })
    
    if (app.globalData.isLogin) {
      this.loadRecords()
    }
  },

  onShow() {
    this.setData({ isLogin: app.globalData.isLogin })
  },

  // 获取当前月份
  getCurrentMonth() {
    const now = new Date()
    return `${now.getFullYear()}年${now.getMonth() + 1}月`
  },

  // 加载运动记录
  async loadRecords() {
    this.setData({ loading: true })
    
    try {
      // 计算日期范围：最近30天
      const endDate = formatDate(new Date())
      const startDateObj = new Date()
      startDateObj.setDate(startDateObj.getDate() - 29)
      const startDate = formatDate(startDateObj)
      
      // 调用真实 API 获取步数记录
      const res = await api.getDailySteps({ startDate, endDate })
      
      if (res.code === 200 && res.data) {
        const today = new Date()
        const records = (res.data || []).map((item, index) => {
          const date = new Date(item.recordDate)
          const steps = item.validSteps || item.rawSteps || 0
          const mileage = (steps / this.data.stepToKmRate).toFixed(2)
          return {
            id: item.id || index + 1,
            date: item.recordDate,
            dateText: getDateText(date, today),
            weekDay: getWeekDay(date),
            steps: steps,
            mileage: parseFloat(mileage),
            calories: Math.floor(steps * 0.04),
            duration: Math.floor(steps / 100)
          }
        })
        
        // 计算统计数据
        let totalSteps = 0
        let totalMileage = 0
        records.forEach(r => {
          totalSteps += r.steps
          totalMileage += r.mileage
        })
        
        const totalDays = records.filter(r => r.steps > 0).length
        const avgSteps = totalDays > 0 ? Math.floor(totalSteps / totalDays) : 0
        
        this.setData({
          records,
          totalSteps,
          totalMileage: totalMileage.toFixed(2),
          totalDays,
          avgSteps,
          loading: false
        })
      } else {
        this.setData({ loading: false, records: [] })
      }
    } catch (e) {
      console.error('加载记录失败', e)
      this.setData({ loading: false, records: [] })
    }
  },

  // 去登录
  goLogin() {
    wx.navigateTo({
      url: '/pages/authorize/authorize'
    })
  },

  // 点击记录查看详情
  onRecordTap(e) {
    const { record } = e.currentTarget.dataset
    wx.showModal({
      title: record.dateText + ' ' + record.weekDay,
      content: `步数：${record.steps} 步\n里程：${record.mileage} 公里\n消耗：${record.calories} 千卡\n时长：约 ${record.duration} 分钟`,
      showCancel: false,
      confirmText: '知道了'
    })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadRecords()
    wx.stopPullDownRefresh()
  },

  // 分享
  onShareAppMessage() {
    return {
      title: `我已累计行走 ${this.data.totalMileage} 公里，快来一起重走长征路！`,
      path: '/pages/index/index'
    }
  }
})
