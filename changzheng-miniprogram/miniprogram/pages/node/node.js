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
      const nodeId = options.nodeId
      this.setData({ nodeId })
      // 先立即显示本地数据（快速响应）
      this.useLocalNodeData(nodeId)
      this.setData({ loading: false })
      // 后台异步加载服务端数据
      this.loadServerData(nodeId)
    } else {
      wx.showToast({
        title: '节点不存在',
        icon: 'none'
      })
      setTimeout(() => wx.navigateBack(), 1500)
    }
  },

  // 页面显示时刷新数据（从内容页返回后）
  onShow() {
    if (this.data.nodeId) {
      this.loadServerData(this.data.nodeId)
    }
  },

  // 后台加载服务端数据
  async loadServerData(nodeId) {
    try {
      const nodeRes = await api.getNodeDetail(nodeId)
      if (nodeRes.code === 200 && nodeRes.data) {
        this.setData({ nodeInfo: nodeRes.data })
        wx.setNavigationBarTitle({
          title: nodeRes.data.nodeName || '节点详情'
        })
      }
      
      // 加载内容列表
      const contentsRes = await api.getNodeContents(nodeId)
      if (contentsRes.code === 200 && contentsRes.data) {
        const contents = contentsRes.data || []
        const learnedCount = contents.filter(c => c.isLearned).length
        this.setData({
          contents,
          totalCount: contents.length,
          learnedCount,
          progressPercent: contents.length > 0 ? Math.round(learnedCount / contents.length * 100) : 0
        })
      }
    } catch (e) {
      console.log('加载服务端数据失败', e)
    }
  },

  // 使用本地节点数据
  useLocalNodeData(nodeId) {
    // 不再使用本地数据，直接显示加载状态
    wx.setNavigationBarTitle({
      title: '节点详情'
    })
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
