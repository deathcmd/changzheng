// pages/bind/bind.js
const app = getApp()
const api = require('../../utils/api')
const config = require('../../config/index')

// 使用配置文件中的Mock开关
const USE_MOCK = config.useMock

// Mock学生数据库
const MOCK_STUDENTS = [
  { studentNo: '2024031294', name: '李荣为', major: '软件技术', className: '软件24349', grade: '2024级' },
  { studentNo: '20230001', name: '张三', major: '软件技术', className: '软件2301', grade: '2023级' },
  { studentNo: '20230002', name: '李四', major: '软件技术', className: '软件2301', grade: '2023级' },
  { studentNo: '20230003', name: '王五', major: '计算机应用技术', className: '计应2301', grade: '2023级' },
  { studentNo: '20230004', name: '赵六', major: '大数据技术', className: '大数据2301', grade: '2023级' },
  { studentNo: '20230005', name: '钱七', major: '物联网应用技术', className: '物联网2301', grade: '2023级' }
]

// 已绑定的学号列表（Mock模拟）
let boundStudentNos = wx.getStorageSync('boundStudentNos') || []

Page({
  data: {
    // 表单数据
    studentNo: '',
    name: '',
    
    // 状态
    loading: false,
    
    // 已绑定信息
    isBound: false,
    boundInfo: null
  },

  onLoad() {
    this.checkBindStatus()
  },

  onShow() {
    // 每次显示页面时重新检查绑定状态
    this.checkBindStatus()
  },

  // 检查绑定状态
  checkBindStatus() {
    // 从Storage重新加载最新的用户信息
    const storedUserInfo = wx.getStorageSync('userInfo')
    if (storedUserInfo) {
      app.globalData.userInfo = storedUserInfo
    }
    
    const userInfo = app.globalData.userInfo
    if (userInfo && userInfo.studentNo) {
      this.setData({
        isBound: true,
        boundInfo: {
          studentNo: userInfo.studentNo,
          name: userInfo.name || userInfo.nickName,
          major: userInfo.major || '',
          className: userInfo.className || '',
          grade: userInfo.grade || '',
          college: userInfo.college || '智能制造与信息工程学院'
        }
      })
    } else {
      this.setData({
        isBound: false,
        boundInfo: null
      })
    }
  },

  // 学号输入
  onStudentNoInput(e) {
    this.setData({
      studentNo: e.detail.value.trim()
    })
  },

  // 姓名输入
  onNameInput(e) {
    this.setData({
      name: e.detail.value.trim()
    })
  },

  // 提交认证
  async onSubmit() {
    const { studentNo, name } = this.data
    
    // 验证
    if (!studentNo) {
      wx.showToast({ title: '请输入学号', icon: 'none' })
      return
    }
    if (!name) {
      wx.showToast({ title: '请输入姓名', icon: 'none' })
      return
    }
    
    this.setData({ loading: true })
    
    try {
      if (USE_MOCK) {
        await new Promise(resolve => setTimeout(resolve, 800))
        
        // 在Mock数据中查找
        const student = MOCK_STUDENTS.find(s => s.studentNo === studentNo && s.name === name)
        
        if (!student) {
          wx.showModal({
            title: '认证失败',
            content: '学号或姓名不匹配，请确认信息是否正确。\n\n如有问题请联系辅导员。',
            showCancel: false
          })
          this.setData({ loading: false })
          return
        }
        
        // 检查学号是否已被其他账号绑定
        const currentUserId = app.globalData.userInfo?.id || app.globalData.userInfo?.openId
        if (boundStudentNos.includes(studentNo)) {
          // 检查是否是当前用户绑定的
          const existingBind = wx.getStorageSync('studentBindMap') || {}
          if (existingBind[studentNo] && existingBind[studentNo] !== currentUserId) {
            wx.showModal({
              title: '认证失败',
              content: '该学号已被其他账号绑定，一个学号只能绑定一个账号。\n\n如有问题请联系辅导员。',
              showCancel: false
            })
            this.setData({ loading: false })
            return
          }
        }
        
        // 记录绑定关系
        if (!boundStudentNos.includes(studentNo)) {
          boundStudentNos.push(studentNo)
          wx.setStorageSync('boundStudentNos', boundStudentNos)
        }
        const bindMap = wx.getStorageSync('studentBindMap') || {}
        bindMap[studentNo] = currentUserId
        wx.setStorageSync('studentBindMap', bindMap)
        
        // 认证成功，更新全局数据
        app.globalData.userInfo = {
          ...app.globalData.userInfo,
          studentNo: student.studentNo,
          studentId: student.studentNo,
          name: student.name,
          major: student.major,
          className: student.className,
          grade: student.grade,
          college: '智能制造与信息工程学院'
        }
        
        // 持久化保存到Storage
        wx.setStorageSync('userInfo', app.globalData.userInfo)
        
        this.setData({
          loading: false,
          isBound: true,
          boundInfo: student
        })
        
        wx.showToast({
          title: '认证成功',
          icon: 'success',
          duration: 1500
        })
        
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
        
        return
      }
      
      // 真实API调用
      const res = await api.bindStudent({ studentNo, name })
      
      if (res.code === 200) {
        app.globalData.userInfo = {
          ...app.globalData.userInfo,
          ...res.data
        }
        
        // 持久化保存到Storage
        wx.setStorageSync('userInfo', app.globalData.userInfo)
        
        wx.showToast({
          title: '认证成功',
          icon: 'success',
          duration: 1500
        })
        
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        wx.showModal({
          title: '认证失败',
          content: res.message || '学号或姓名不匹配',
          showCancel: false
        })
      }
    } catch (e) {
      console.error('认证失败', e)
      wx.showToast({ title: '认证失败，请重试', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
