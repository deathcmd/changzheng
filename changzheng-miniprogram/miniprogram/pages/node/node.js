// pages/node/node.js
const app = getApp()
const api = require('../../utils/api')

Page({
  data: {
    nodeId: null,
    nodeInfo: null,
    contents: [],
    loading: true,
    // 学习进度
    learnedCount: 0,
    totalCount: 0,
    progressPercent: 0
  },

  onLoad(options) {
    if (options.nodeId) {
      this.setData({ nodeId: options.nodeId })
      this.loadNodeData(options.nodeId)
    } else {
      wx.showToast({
        title: '节点不存在',
        icon: 'none'
      })
      setTimeout(() => wx.navigateBack(), 1500)
    }
  },

  // 加载节点数据
  async loadNodeData(nodeId) {
    this.setData({ loading: true })
    
    try {
      const [nodeRes, contentsRes] = await Promise.all([
        api.getNodeDetail(nodeId),
        api.getNodeContents(nodeId)
      ])
      
      if (nodeRes.code === 200 && nodeRes.data) {
        this.setData({ nodeInfo: nodeRes.data })
        // 设置页面标题
        wx.setNavigationBarTitle({
          title: nodeRes.data.nodeName || '节点详情'
        })
      }
      
      if (contentsRes.code === 200 && contentsRes.data) {
        const contents = contentsRes.data || []
        const learnedCount = contents.filter(c => c.isLearned).length
        const progressPercent = contents.length > 0 ? Math.round(learnedCount / contents.length * 100) : 0
        this.setData({
          contents,
          totalCount: contents.length,
          learnedCount,
          progressPercent
        })
      }
    } catch (e) {
      console.error('加载节点数据失败', e)
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 点击内容项
  onContentTap(e) {
    const { item } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/content/content?contentId=${item.id}&nodeId=${this.data.nodeId}`
    })
  },

  // 分享
  onShareAppMessage() {
    const { nodeInfo } = this.data
    return {
      title: `我到达了【${nodeInfo?.nodeName || '长征节点'}】`,
      path: `/pages/node/node?nodeId=${this.data.nodeId}`,
      imageUrl: nodeInfo?.coverUrl || '/images/share-cover.png'
    }
  }
})
