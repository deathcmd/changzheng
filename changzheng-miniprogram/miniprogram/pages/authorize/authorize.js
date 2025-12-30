// pages/authorize/authorize.js
const app = getApp()
const config = require('../../config/index')

// 使用配置文件中的Mock开关
const USE_MOCK = config.useMock

// Mock学生数据库
const MOCK_STUDENTS = [
  { studentNo: '2024031294', name: '李荣为', major: '软件技术', className: '软件24349', grade: '2024级', bindUserId: null },
  { studentNo: '20230001', name: '张三', major: '软件技术', className: '软件2301', grade: '2023级', bindUserId: null },
  { studentNo: '20230002', name: '李四', major: '软件技术', className: '软件2301', grade: '2023级', bindUserId: null },
  { studentNo: '20230003', name: '王五', major: '计算机应用技术', className: '计应2301', grade: '2023级', bindUserId: null },
  { studentNo: '20230004', name: '赵六', major: '大数据技术', className: '大数据2301', grade: '2023级', bindUserId: null },
  { studentNo: '20230005', name: '钱七', major: '物联网应用技术', className: '物联网2301', grade: '2023级', bindUserId: null }
]

// 已绑定的学号列表（Mock模拟）
let boundStudentNos = wx.getStorageSync('boundStudentNos') || []

Page({
  data: {
    loading: false,
    agreed: false, // 是否同意协议
    step: 'login', // login | profile | bindStudent
    tempAvatarUrl: '', // 临时头像
    tempNickName: '', // 临时昵称
    studentNo: '', // 学号
    studentName: '' // 姓名
  },

  onLoad(options) {
    // 检查是否已登录
    if (app.globalData.isLogin) {
      this.goBack()
    }
  },

  // 同意协议
  onAgreeChange(e) {
    this.setData({ agreed: e.detail.value.length > 0 })
  },

  // 微信授权登录
  async onWxLogin() {
    if (!this.data.agreed) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      })
      return
    }

    if (this.data.loading) return

    this.setData({ loading: true })

    try {
      await app.wxLogin()
      
      wx.showToast({
        title: '登录成功',
        icon: 'success'
      })

      // 登录成功后检查是否已有头像昵称
      const userInfo = app.globalData.userInfo
      if (userInfo.avatarUrl && userInfo.nickName) {
        // 已有头像昵称，检查是否需要学生认证
        if (userInfo.studentNo) {
          // 已认证，直接返回
          setTimeout(() => this.goBack(), 1000)
        } else {
          // 进入学生认证步骤
          this.setData({ step: 'bindStudent' })
        }
      } else {
        // 进入设置头像昵称步骤
        this.setData({ step: 'profile' })
      }
    } catch (e) {
      console.error('登录失败', e)
      wx.showToast({
        title: e.message || '登录失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
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

  // 保存头像昵称
  async onSaveProfile() {
    const { tempAvatarUrl, tempNickName } = this.data
    
    // 更新全局数据
    if (tempAvatarUrl || tempNickName) {
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
          const api = require('../../utils/api')
          await api.updateUserProfile({
            nickName: tempNickName,
            avatarUrl: tempAvatarUrl
          })
          console.log('头像昵称已同步到后端')
        } catch (e) {
          console.error('同步头像昵称到后端失败', e)
          // 即使同步失败也继续流程
        }
      }
    }
    
    // 进入学生认证步骤
    this.setData({ step: 'bindStudent' })
  },

  // 跳过设置头像昵称
  onSkipProfile() {
    this.setData({ step: 'bindStudent' })
  },

  // 学号输入
  onStudentIdInput(e) {
    this.setData({ studentNo: e.detail.value })
  },

  // 姓名输入
  onNameInput(e) {
    this.setData({ studentName: e.detail.value })
  },

  // 绑定学生
  async onBindStudent() {
    const { studentNo, studentName } = this.data
    
    if (!studentNo) {
      wx.showToast({ title: '请输入学号', icon: 'none' })
      return
    }
    if (!studentName) {
      wx.showToast({ title: '请输入姓名', icon: 'none' })
      return
    }
    
    if (this.data.loading) return
    this.setData({ loading: true })
    
    try {
      if (USE_MOCK) {
        await new Promise(resolve => setTimeout(resolve, 800))
        
        // 在Mock数据中查找
        const student = MOCK_STUDENTS.find(s => s.studentNo === studentNo && s.name === studentName)
        
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
        
        wx.showToast({
          title: '认证成功',
          icon: 'success',
          duration: 1500
        })
        
        setTimeout(() => this.goBack(), 1500)
        return
      }
      
      // 真实API调用
      const api = require('../../utils/api')
      const result = await api.bindStudent({
        studentNo,
        name: studentName
      })
      
      // 更新全局数据
      app.globalData.userInfo = {
        ...app.globalData.userInfo,
        studentNo: result.studentNo || studentNo,
        studentId: result.studentId,
        name: result.name || studentName,
        major: result.major,
        className: result.className,
        grade: result.grade,
        college: result.college || '智能制造与信息工程学院'
      }
      
      // 持久化保存
      wx.setStorageSync('userInfo', app.globalData.userInfo)
      
      wx.showToast({
        title: '认证成功',
        icon: 'success'
      })
      
      setTimeout(() => this.goBack(), 1500)
    } catch (e) {
      console.error('绑定失败', e)
      wx.showToast({
        title: e.message || '认证失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 跳过绑定学号
  onSkipBind() {
    wx.showModal({
      title: '提示',
      content: '跳过绑定学号将无法参与班级/年级排行榜，确定跳过吗？',
      success: (res) => {
        if (res.confirm) {
          this.goBack()
        }
      }
    })
  },

  // 返回上一页或首页
  goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.switchTab({
        url: '/pages/index/index'
      })
    }
  },

  // 查看用户协议
  onViewAgreement() {
    wx.showModal({
      title: '用户协议',
      content: '请仔细阅读并理解用户协议内容...\n\n1. 服务条款\n2. 隐私政策\n3. 免责声明\n\n详细内容请访问官网查看。',
      showCancel: false
    })
  },

  // 查看隐私政策
  onViewPrivacy() {
    wx.showModal({
      title: '隐私政策',
      content: '我们重视您的隐私保护...\n\n本应用会收集以下信息：\n1. 微信运动步数\n2. 微信头像昵称\n3. 学号信息（可选）\n\n我们承诺不会将您的信息用于其他用途。',
      showCancel: false
    })
  }
})
