// pages/profile/profile.js
const app = getApp()
const api = require('../../utils/api')
const config = require('../../config/index')

Page({
  data: {
    avatarUrl: '',
    nickName: '',
    tempAvatarUrl: '',
    tempNickName: '',
    loading: false
  },

  onLoad() {
    this.loadUserInfo()
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = app.globalData.userInfo || {}
    this.setData({
      avatarUrl: userInfo.avatarUrl || '',
      nickName: userInfo.nickName || '',
      tempAvatarUrl: userInfo.avatarUrl || '',
      tempNickName: userInfo.nickName || ''
    })
  },

  // 获取用户头像
  onChooseAvatar(e) {
    const { avatarUrl } = e.detail
    this.setData({ tempAvatarUrl: avatarUrl })
  },

  // 昵称输入
  onNickNameInput(e) {
    this.setData({ tempNickName: e.detail.value })
  },

  // 昵称输入框失去焦点
  onNickNameBlur(e) {
    this.setData({ tempNickName: e.detail.value })
  },

  // 保存资料
  async onSave() {
    const { tempAvatarUrl, tempNickName, avatarUrl, nickName } = this.data
    
    // 检查是否有修改
    if (tempAvatarUrl === avatarUrl && tempNickName === nickName) {
      wx.showToast({ title: '未做任何修改', icon: 'none' })
      return
    }
    
    if (this.data.loading) return
    this.setData({ loading: true })
    
    try {
      // 更新全局数据
      app.globalData.userInfo = {
        ...app.globalData.userInfo,
        avatarUrl: tempAvatarUrl || app.globalData.userInfo.avatarUrl,
        nickName: tempNickName || app.globalData.userInfo.nickName
      }
      
      // 持久化保存
      wx.setStorageSync('userInfo', app.globalData.userInfo)
      
      // 同步到后端(非Mock模式)
      if (!config.useMock && app.globalData.isLogin) {
        try {
          await api.updateUserProfile({
            nickName: tempNickName,
            avatarUrl: tempAvatarUrl
          })
          console.log('头像昵称已同步到后端')
        } catch (e) {
          console.error('同步头像昵称到后端失败', e)
          // 即使同步失败也继续，本地已保存
        }
      }
      
      this.setData({
        avatarUrl: tempAvatarUrl,
        nickName: tempNickName
      })
      
      wx.showToast({
        title: '保存成功',
        icon: 'success',
        duration: 1500
      })
      
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    } catch (e) {
      console.error('保存失败', e)
      wx.showToast({ title: '保存失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 取消
  onCancel() {
    wx.navigateBack()
  }
})
