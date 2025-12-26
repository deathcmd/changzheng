// pages/achievement/achievement.js
const app = getApp()
const api = require('../../utils/api')

Page({
  data: {
    achievements: [],
    unlockedCount: 0,
    totalCount: 0,
    progressPercent: 0,
    loading: true
  },

  onLoad() {
    this.loadAchievements()
  },

  // 加载成就数据
  async loadAchievements() {
    this.setData({ loading: true })
    
    try {
      const res = await api.getUserAchievements()
      if (res.code === 200 && res.data) {
        const achievements = res.data || []
        const unlockedCount = achievements.filter(a => a.isUnlocked).length
        
        // 按解锁状态排序：已解锁在前
        achievements.sort((a, b) => {
          if (a.isUnlocked && !b.isUnlocked) return -1
          if (!a.isUnlocked && b.isUnlocked) return 1
          return 0
        })
        
        this.setData({
          achievements,
          unlockedCount,
          totalCount: achievements.length,
          progressPercent: achievements.length > 0 ? Math.round(unlockedCount / achievements.length * 100) : 0
        })
      }
    } catch (e) {
      console.error('加载成就失败', e)
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 点击成就
  onAchievementTap(e) {
    const { item } = e.currentTarget.dataset
    
    if (item.isUnlocked) {
      // 显示成就详情
      wx.showModal({
        title: item.name,
        content: `${item.description}\n\n获得时间：${item.unlockTime || '未知'}`,
        showCancel: false
      })
    } else {
      wx.showToast({
        title: item.condition || '继续努力解锁',
        icon: 'none'
      })
    }
  },

  // 分享
  onShareAppMessage() {
    return {
      title: `我已获得${this.data.unlockedCount}个长征成就！`,
      path: '/pages/achievement/achievement',
      imageUrl: '/images/share-achievement.png'
    }
  }
})
