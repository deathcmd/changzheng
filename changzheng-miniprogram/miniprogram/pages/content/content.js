// pages/content/content.js
const app = getApp()
const api = require('../../utils/api')

Page({
  data: {
    contentId: null,
    nodeId: null,
    contentInfo: null,
    loading: true,
    isLearned: false,
    // 视频/音频播放状态
    playing: false,
    currentTime: 0,
    duration: 0
  },

  onLoad(options) {
    if (options.contentId) {
      this.setData({
        contentId: options.contentId,
        nodeId: options.nodeId || null
      })
      this.loadContentData(options.contentId)
    }
  },

  onUnload() {
    // 页面销毁时标记已学习
    if (!this.data.isLearned && this.data.contentId) {
      this.markAsLearned()
    }
  },

  // 加载内容数据
  async loadContentData(contentId) {
    this.setData({ loading: true })
    
    try {
      const res = await api.getContentDetail(contentId)
      if (res.code === 200 && res.data) {
        this.setData({
          contentInfo: res.data,
          isLearned: res.data.isLearned || false
        })
        wx.setNavigationBarTitle({
          title: res.data.title || '学习内容'
        })
      }
    } catch (e) {
      console.error('加载内容失败', e)
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 标记为已学习
  async markAsLearned() {
    if (this.data.isLearned) return
    
    try {
      await api.markContentLearned(this.data.contentId)
      this.setData({ isLearned: true })
    } catch (e) {
      console.error('标记已学习失败', e)
    }
  },

  // 视频播放
  onVideoPlay() {
    this.setData({ playing: true })
  },

  // 视频暂停
  onVideoPause() {
    this.setData({ playing: false })
  },

  // 视频结束
  onVideoEnded() {
    this.setData({ playing: false })
    this.markAsLearned()
    wx.showToast({
      title: '学习完成',
      icon: 'success'
    })
  },

  // 视频时间更新
  onVideoTimeUpdate(e) {
    const { currentTime, duration } = e.detail
    this.setData({ currentTime, duration })
    
    // 观看超过80%标记为已学习
    if (duration > 0 && currentTime / duration > 0.8 && !this.data.isLearned) {
      this.markAsLearned()
    }
  },

  // 完成学习按钮
  onCompleteTap() {
    this.markAsLearned()
    wx.showToast({
      title: '学习完成',
      icon: 'success'
    })
    setTimeout(() => {
      wx.navigateBack()
    }, 1500)
  },

  // 分享
  onShareAppMessage() {
    const { contentInfo } = this.data
    return {
      title: contentInfo?.title || '长征学习内容',
      path: `/pages/content/content?contentId=${this.data.contentId}`,
      imageUrl: contentInfo?.coverUrl || '/images/share-cover.png'
    }
  }
})
