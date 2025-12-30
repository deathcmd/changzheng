// pages/achievement/achievement.js
const app = getApp()
const api = require('../../utils/api')

// 长征节点成就定义
const ACHIEVEMENTS = [
  { id: 1, name: '突破乌江', km: 0, icon: '🚩', description: '开启长征之路' },
  { id: 2, name: '遵义会议', km: 525, icon: '⭐', description: '历史转折点' },
  { id: 3, name: '四渡赤水', km: 1050, icon: '🌊', description: '奇兵出奇制' },
  { id: 4, name: '巧渡金沙江', km: 1550, icon: '⛵', description: '跨越天堔' },
  { id: 5, name: '强渡大渡河', km: 2050, icon: '💪', description: '勇往直前' },
  { id: 6, name: '飞夺泸定桥', km: 2530, icon: '🔥', description: '险中求胜' },
  { id: 7, name: '翻越夹金山', km: 3080, icon: '❄️', description: '雪山英雄' },
  { id: 8, name: '走过草地', km: 3650, icon: '🌿', description: '艰苦卓绝' },
  { id: 9, name: '突破腊子口', km: 4200, icon: '⚔️', description: '决战时刻' },
  { id: 10, name: '吴起镇会师', km: 4750, icon: '🤝', description: '两军会师' },
  { id: 11, name: '到达吴起镇', km: 5000, icon: '🏆', description: '胜利在望' },
  { id: 12, name: '红军会师', km: 6250, icon: '🌟', description: '三大主力会师' },
  { id: 13, name: '长征胜利', km: 12500, icon: '🎉', description: '伟大胜利' }
]

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
      const res = await api.getUserNodeProgress()
      if (res.code === 200 && res.data) {
        const totalMileage = res.data.totalMileage || 0
        
        // 根据里程计算解锁的成就
        const achievements = ACHIEVEMENTS.map(a => ({
          ...a,
          isUnlocked: totalMileage >= a.km,
          unlockTime: totalMileage >= a.km ? '已解锁' : null
        }))
        
        const unlockedCount = achievements.filter(a => a.isUnlocked).length
        
        // 排序：已解锁在前
        achievements.sort((a, b) => {
          if (a.isUnlocked && !b.isUnlocked) return -1
          if (!a.isUnlocked && b.isUnlocked) return 1
          return a.km - b.km
        })
        
        this.setData({
          achievements,
          unlockedCount,
          totalCount: achievements.length,
          progressPercent: Math.round(unlockedCount / achievements.length * 100)
        })
      }
    } catch (e) {
      console.error('加载成就失败', e)
      // 使用默认数据
      this.setData({
        achievements: ACHIEVEMENTS.map(a => ({ ...a, isUnlocked: false })),
        totalCount: ACHIEVEMENTS.length
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 点击成就
  onAchievementTap(e) {
    const { item } = e.currentTarget.dataset
    
    if (item.isUnlocked) {
      wx.showModal({
        title: `${item.icon} ${item.name}`,
        content: item.description,
        showCancel: false
      })
    } else {
      wx.showToast({
        title: `还需走${item.km}km解锁`,
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
