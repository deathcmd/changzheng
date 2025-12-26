// pages/map/map.js
const app = getApp()
const api = require('../../utils/api')
const util = require('../../utils/util')

// 长征路线节点经纬度数据（按实际地理位置）
const ROUTE_COORDINATES = [
  { id: 1, name: '瑞金', lat: 25.8847, lng: 116.0279, km: 0 },
  { id: 2, name: '于都', lat: 25.9522, lng: 115.4153, km: 60 },
  { id: 3, name: '信丰', lat: 27.8196, lng: 114.9308, km: 200 },
  { id: 4, name: '湘江', lat: 25.2744, lng: 111.0067, km: 400 },
  { id: 5, name: '遂川', lat: 26.3274, lng: 105.2283, km: 800 },
  { id: 6, name: '遵义', lat: 27.7256, lng: 106.9271, km: 1200 },
  { id: 7, name: '赤水河', lat: 28.0167, lng: 105.7000, km: 1500 },
  { id: 8, name: '金沙江', lat: 26.6167, lng: 103.2667, km: 2500 },
  { id: 9, name: '大渡河', lat: 29.3242, lng: 102.2344, km: 3200 },
  { id: 10, name: '泸定桥', lat: 29.9167, lng: 102.2333, km: 3500 },
  { id: 11, name: '夹金山', lat: 30.8667, lng: 102.8167, km: 4000 },
  { id: 12, name: '毛儿盖', lat: 32.0333, lng: 102.5000, km: 4500 },
  { id: 13, name: '草地', lat: 33.4333, lng: 102.9667, km: 5000 },
  { id: 14, name: '腊子口', lat: 34.0833, lng: 103.8500, km: 6000 },
  { id: 15, name: '哈达铺', lat: 34.4833, lng: 104.5167, km: 7000 },
  { id: 16, name: '吴起镇', lat: 36.9278, lng: 108.1758, km: 8000 },
  { id: 17, name: '会宁', lat: 35.6928, lng: 105.0536, km: 25000 }
]

Page({
  data: {
    // 视图模式: map | list
    viewMode: 'map',
    
    // 路线节点
    nodes: [],
    // 用户进度
    userProgress: [],
    // 当前里程
    currentMileage: 0,
    // 总里程
    totalDistance: 25000,
    // 剩余里程
    remainingMileage: 25000,
    // 进度百分比
    progressPercent: 0,
    // 已解锁节点数
    unlockedCount: 0,
    // 总节点数
    totalNodes: 0,
    // 滚动位置
    scrollTop: 0,
    // 当前节点索引
    currentNodeIndex: -1,
    // 加载状态
    loading: true,
    // 登录状态
    isLogin: false,
    
    // 地图相关数据
    mapCenter: {
      latitude: 30.5,
      longitude: 105.0
    },
    mapScale: 5,
    markers: [],
    polyline: [],
    selectedNode: null
  },

  onLoad() {
    this.setData({ isLogin: app.globalData.isLogin })
    this.initMapData()
    this.loadData()
  },

  onShow() {
    // 更新登录状态
    this.setData({ isLogin: app.globalData.isLogin })
    
    // 从全局数据同步里程
    this.syncFromGlobalData()
    
    // 刷新进度
    if (!this.data.loading && app.globalData.isLogin) {
      this.loadUserProgress()
    }
  },

  // 从全局数据同步里程
  syncFromGlobalData() {
    const mileage = app.globalData.totalMileage || 0
    if (mileage !== this.data.currentMileage) {
      const progressPercent = Math.min((mileage / this.data.totalDistance * 100), 100).toFixed(1)
      const remainingMileage = Math.max(0, this.data.totalDistance - mileage).toFixed(0)
      
      // 计算已解锁节点
      let unlockedCount = 0
      ROUTE_COORDINATES.forEach(coord => {
        if (coord.km <= mileage) unlockedCount++
      })
      
      this.setData({
        currentMileage: mileage,
        progressPercent,
        remainingMileage,
        unlockedCount
      })
      
      // 更新地图标记
      this.updateMarkers()
    }
  },

  // 加载数据
  async loadData() {
    this.setData({ loading: true })
    
    try {
      // 先从全局数据获取里程
      const mileage = app.globalData.totalMileage || 0
      const progressPercent = Math.min((mileage / this.data.totalDistance * 100), 100).toFixed(1)
      const remainingMileage = Math.max(0, this.data.totalDistance - mileage).toFixed(0)
      
      // 计算已解锁节点
      let unlockedCount = 0
      ROUTE_COORDINATES.forEach(coord => {
        if (coord.km <= mileage) unlockedCount++
      })
      
      this.setData({
        currentMileage: mileage,
        progressPercent,
        remainingMileage,
        unlockedCount,
        totalNodes: ROUTE_COORDINATES.length
      })
      
      const [nodesRes, progressRes] = await Promise.all([
        api.getRouteNodes(),
        app.globalData.isLogin ? api.getUserNodeProgress() : Promise.resolve({ code: 200, data: { currentMileage: mileage, nodes: [] } })
      ])
      
      if (nodesRes.code === 200 && nodesRes.data) {
        const nodes = nodesRes.data || []
        this.setData({
          nodes,
          totalNodes: nodes.length || ROUTE_COORDINATES.length
        })
      }
      
      if (progressRes.code === 200 && progressRes.data) {
        this.processProgress(progressRes.data)
      }
      
      // 更新地图标记
      this.updateMarkers()
      
    } catch (e) {
      console.error('加载数据失败', e)
      // 即使加载失败，也使用全局数据
      this.syncFromGlobalData()
    } finally {
      this.setData({ loading: false })
    }
  },

  // 加载用户进度
  async loadUserProgress() {
    if (!app.globalData.isLogin) {
      // 未登录也同步全局数据
      this.syncFromGlobalData()
      return
    }
    
    try {
      const res = await api.getUserNodeProgress()
      if (res.code === 200 && res.data) {
        this.processProgress(res.data)
      }
    } catch (e) {
      console.error('加载进度失败', e)
      // 失败时使用全局数据
      this.syncFromGlobalData()
    }
  },

  // 处理进度数据
  processProgress(data) {
    const { currentMileage, unlockedNodes } = data
    const unlockedIds = (unlockedNodes || []).map(n => n.nodeId)
    const mileage = currentMileage || 0
    
    // 计算进度百分比和剩余里程
    const progressPercent = Math.min((mileage / this.data.totalDistance * 100), 100).toFixed(1)
    const remainingMileage = Math.max(0, this.data.totalDistance - mileage).toFixed(0)
    
    // 更新节点状态
    const nodes = this.data.nodes.map((node, index) => {
      const isUnlocked = unlockedIds.includes(node.id)
      const isCurrent = node.cumulativeKm <= mileage && 
        (index === this.data.nodes.length - 1 || this.data.nodes[index + 1].cumulativeKm > mileage)
      const remainingKm = Math.max(0, node.cumulativeKm - mileage).toFixed(1)
      return {
        ...node,
        isUnlocked,
        isCurrent,
        remainingKm
      }
    })
    
    // 计算当前节点索引
    let currentNodeIndex = -1
    for (let i = 0; i < nodes.length; i++) {
      if (nodes[i].isCurrent) {
        currentNodeIndex = i
        break
      }
    }
    
    this.setData({
      nodes,
      currentMileage: mileage,
      remainingMileage,
      progressPercent,
      unlockedCount: unlockedIds.length,
      currentNodeIndex
    })
    
    // 滚动到当前节点
    if (currentNodeIndex >= 0) {
      this.scrollToCurrentNode(currentNodeIndex)
    }
    
    // 更新地图标记
    this.updateMarkers()
  },

  // 滚动到当前节点
  scrollToCurrentNode(index) {
    // 计算滚动位置
    const itemHeight = 180 // 每个节点的大致高度
    const scrollTop = Math.max(0, (index - 2) * itemHeight)
    this.setData({ scrollTop })
  },

  // 点击节点
  onNodeTap(e) {
    const { node } = e.currentTarget.dataset
    
    if (!node.isUnlocked) {
      wx.showToast({
        title: `还需行进 ${(node.cumulativeKm - this.data.currentMileage).toFixed(1)} km 解锁`,
        icon: 'none'
      })
      return
    }
    
    wx.navigateTo({
      url: `/pages/node/node?nodeId=${node.id}`
    })
  },

  // 定位到当前位置
  locateToMe() {
    if (this.data.currentNodeIndex >= 0) {
      this.scrollToCurrentNode(this.data.currentNodeIndex)
    }
  },

  // 计算进度百分比
  getProgress() {
    const { currentMileage, totalDistance } = this.data
    return Math.min((currentMileage / totalDistance * 100), 100).toFixed(1)
  },

  // 去登录
  goLogin() {
    wx.navigateTo({
      url: '/pages/authorize/authorize'
    })
  },

  // 初始化地图数据
  initMapData() {
    // 生成路线
    const points = ROUTE_COORDINATES.map(coord => ({
      latitude: coord.lat,
      longitude: coord.lng
    }))
    
    const polyline = [{
      points: points,
      color: '#C41E3A',
      width: 4,
      dottedLine: false,
      arrowLine: true,
      borderColor: '#FFFFFF',
      borderWidth: 1
    }]
    
    this.setData({ polyline })
    this.updateMarkers()
  },

  // 更新地图标记
  updateMarkers() {
    const { nodes, currentMileage } = this.data
    const unlockedIds = nodes.filter(n => n.isUnlocked).map(n => n.id)
    
    const markers = ROUTE_COORDINATES.map((coord, index) => {
      const node = nodes.find(n => n.id === coord.id) || coord
      const isUnlocked = unlockedIds.includes(coord.id) || coord.km <= currentMileage
      const isCurrent = coord.km <= currentMileage && 
        (index === ROUTE_COORDINATES.length - 1 || ROUTE_COORDINATES[index + 1].km > currentMileage)
      
      // 根据状态确定标记样式
      let labelBgColor = '#E0E0E0' // 未解锁 - 灰色
      let labelColor = '#999999'
      let statusIcon = '🔒' // 锁定
      let calloutBgColor = '#F5F5F5'
      
      if (isCurrent) {
        labelBgColor = '#FF9500' // 当前节点 - 橙色
        labelColor = '#FFFFFF'
        statusIcon = '📍' // 当前位置
        calloutBgColor = '#FFF3E0'
      } else if (isUnlocked) {
        labelBgColor = '#52C41A' // 已解锁 - 绿色
        labelColor = '#FFFFFF'
        statusIcon = '✅' // 已完成
        calloutBgColor = '#F6FFED'
      }
      
      return {
        id: coord.id,
        latitude: coord.lat,
        longitude: coord.lng,
        title: coord.name,
        width: isCurrent ? 36 : 28,
        height: isCurrent ? 36 : 28,
        label: {
          content: `${statusIcon} ${coord.name}`,
          color: labelColor,
          fontSize: isCurrent ? 12 : 11,
          fontWeight: isCurrent ? 'bold' : 'normal',
          anchorX: 0,
          anchorY: -10,
          bgColor: labelBgColor,
          padding: 5,
          borderRadius: 6
        },
        callout: {
          content: `${coord.name}\n${statusIcon} ${isUnlocked ? '已解锁' : '未解锁'}\n里程：${coord.km} km`,
          color: '#333333',
          fontSize: 12,
          borderRadius: 8,
          padding: 8,
          display: isCurrent ? 'ALWAYS' : 'BYCLICK',
          bgColor: calloutBgColor,
          textAlign: 'center'
        }
      }
    })
    
    // 添加用户当前位置标记（在路线上的精确位置）
    const userPosition = this.calculateUserPosition(currentMileage)
    if (userPosition && currentMileage > 0) {
      markers.push({
        id: 999,
        latitude: userPosition.lat,
        longitude: userPosition.lng,
        title: '我的位置',
        width: 40,
        height: 40,
        zIndex: 100,
        anchor: { x: 0.5, y: 0.5 },
        label: {
          content: '🚶',
          color: '#C41E3A',
          fontSize: 24,
          anchorX: -12,
          anchorY: -12
        },
        callout: {
          content: `🚩 已行进 ${currentMileage.toFixed(1)} km\n继续加油！`,
          color: '#C41E3A',
          fontSize: 13,
          fontWeight: 'bold',
          borderRadius: 10,
          padding: 10,
          display: 'ALWAYS',
          bgColor: '#FFFFFF',
          textAlign: 'center'
        }
      })
    }
    
    this.setData({ markers })
  },

  // 计算用户在路线上的精确位置
  calculateUserPosition(mileage) {
    if (mileage <= 0) {
      return { lat: ROUTE_COORDINATES[0].lat, lng: ROUTE_COORDINATES[0].lng }
    }
    
    // 找到用户当前所在的两个节点之间
    for (let i = 0; i < ROUTE_COORDINATES.length - 1; i++) {
      const current = ROUTE_COORDINATES[i]
      const next = ROUTE_COORDINATES[i + 1]
      
      if (mileage >= current.km && mileage < next.km) {
        // 计算在两点之间的比例
        const ratio = (mileage - current.km) / (next.km - current.km)
        
        // 线性插值计算位置
        const lat = current.lat + (next.lat - current.lat) * ratio
        const lng = current.lng + (next.lng - current.lng) * ratio
        
        return { lat, lng }
      }
    }
    
    // 如果已经超过最后一个节点
    const last = ROUTE_COORDINATES[ROUTE_COORDINATES.length - 1]
    return { lat: last.lat, lng: last.lng }
  },

  // 切换视图
  switchView(e) {
    const mode = e.currentTarget.dataset.mode
    this.setData({ viewMode: mode })
  },

  // 点击地图标记
  onMarkerTap(e) {
    const markerId = e.markerId
    const coord = ROUTE_COORDINATES.find(c => c.id === markerId)
    const node = this.data.nodes.find(n => n.id === markerId)
    
    if (coord) {
      const isUnlocked = node ? node.isUnlocked : (coord.km <= this.data.currentMileage)
      this.setData({
        selectedNode: {
          id: markerId,
          nodeName: coord.name,
          cumulativeKm: coord.km,
          description: node?.description || '长征路上的重要节点',
          isUnlocked: isUnlocked,
          latitude: coord.lat,
          longitude: coord.lng
        }
      })
    }
  },

  // 关闭弹窗
  closePopup() {
    this.setData({ selectedNode: null })
  },

  // 阻止事件冒泡
  stopPropagation() {},

  // 查看节点详情
  goNodeDetail() {
    const { selectedNode } = this.data
    if (selectedNode && selectedNode.isUnlocked) {
      this.closePopup()
      wx.navigateTo({
        url: `/pages/node/node?nodeId=${selectedNode.id}`
      })
    }
  },

  // 放大地图
  zoomIn() {
    let scale = this.data.mapScale + 1
    if (scale > 18) scale = 18
    this.setData({ mapScale: scale })
  },

  // 缩小地图
  zoomOut() {
    let scale = this.data.mapScale - 1
    if (scale < 3) scale = 3
    this.setData({ mapScale: scale })
  },

  // 重置地图
  resetMap() {
    this.setData({
      mapCenter: {
        latitude: 30.5,
        longitude: 105.0
      },
      mapScale: 5
    })
  },

  // 地图区域变化
  onRegionChange(e) {
    // 可以记录地图位置变化
  }
})
