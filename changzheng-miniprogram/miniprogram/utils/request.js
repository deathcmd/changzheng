// utils/request.js - 请求封装
const config = require('../config/index')
const mock = require('./mock')

// 请求队列，用于处理token刷新
let isRefreshing = false
let requestQueue = []

/**
 * 封装请求方法
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    const app = getApp()
    const { url, method = 'GET', data = {}, header = {}, noAuth = false } = options

    // Mock模式
    if (config.useMock) {
      setTimeout(() => {
        const mockRes = mock.getMockResponse(method, url)
        if (config.debug) {
          console.log(`[Mock] ${method} ${url}`, mockRes)
        }
        resolve(mockRes)
      }, 300) // 模拟网络延迟
      return
    }

    // 构建请求头
    const requestHeader = {
      'Content-Type': 'application/json',
      ...header
    }

    // 添加token
    if (!noAuth && app.globalData.token) {
      requestHeader['Authorization'] = `Bearer ${app.globalData.token}`
    }

    // 添加用户ID
    if (app.globalData.userInfo && app.globalData.userInfo.userId) {
      requestHeader['X-User-Id'] = String(app.globalData.userInfo.userId)
      console.log('[Request] Adding X-User-Id:', app.globalData.userInfo.userId)
    } else {
      console.log('[Request] No userId found in userInfo:', app.globalData.userInfo)
    }

    // 完整URL
    const fullUrl = url.startsWith('http') ? url : `${config.baseUrl}${url}`

    if (config.debug) {
      console.log(`[Request] ${method} ${fullUrl}`, data)
    }

    wx.request({
      url: fullUrl,
      method,
      data,
      header: requestHeader,
      timeout: 30000,
      success: (res) => {
        if (config.debug) {
          console.log(`[Response] ${method} ${url}`, res.data)
        }

        const statusCode = res.statusCode
        const responseData = res.data

        // HTTP状态码判断
        if (statusCode >= 200 && statusCode < 300) {
          // 业务状态码判断
          if (responseData.code === 200) {
            resolve(responseData)
          } else if (responseData.code === 40100 || responseData.code === 40101) {
            // Token过期或无效
            handleTokenExpired()
            reject(new Error('登录已过期，请重新登录'))
          } else {
            // 业务错误
            wx.showToast({
              title: responseData.message || responseData.msg || '请求失败',
              icon: 'none',
              duration: 2000
            })
            reject(new Error(responseData.message || responseData.msg || '请求失败'))
          }
        } else if (statusCode === 401) {
          // 未授权
          handleTokenExpired()
          reject(new Error('未授权'))
        } else if (statusCode === 403) {
          wx.showToast({
            title: '没有权限访问',
            icon: 'none'
          })
          reject(new Error('没有权限'))
        } else if (statusCode === 404) {
          reject(new Error('接口不存在'))
        } else if (statusCode >= 500) {
          wx.showToast({
            title: '服务器繁忙，请稍后重试',
            icon: 'none'
          })
          reject(new Error('服务器错误'))
        } else {
          reject(new Error(`请求失败: ${statusCode}`))
        }
      },
      fail: (err) => {
        console.error('[Request Error]', err)
        // 网络错误处理
        if (err.errMsg.includes('timeout')) {
          wx.showToast({
            title: '请求超时，请检查网络',
            icon: 'none'
          })
        } else if (err.errMsg.includes('fail')) {
          wx.showToast({
            title: '网络连接失败',
            icon: 'none'
          })
        }
        reject(err)
      }
    })
  })
}

// 处理Token过期
const handleTokenExpired = () => {
  const app = getApp()
  app.logout()
  
  wx.showModal({
    title: '提示',
    content: '登录已过期，请重新登录',
    showCancel: false,
    confirmText: '确定',
    success: () => {
      wx.reLaunch({
        url: '/pages/authorize/authorize'
      })
    }
  })
}

// GET请求
const get = (url, params = {}, options = {}) => {
  // 将params转为query string
  let queryString = ''
  if (Object.keys(params).length > 0) {
    queryString = '?' + Object.keys(params)
      .filter(key => params[key] !== undefined && params[key] !== null)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
      .join('&')
  }
  
  return request({
    url: url + queryString,
    method: 'GET',
    ...options
  })
}

// POST请求
const post = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  })
}

// PUT请求
const put = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'PUT',
    data,
    ...options
  })
}

// DELETE请求
const del = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'DELETE',
    data,
    ...options
  })
}

// 上传文件
const upload = (url, filePath, name = 'file', formData = {}) => {
  return new Promise((resolve, reject) => {
    const app = getApp()
    const fullUrl = url.startsWith('http') ? url : `${config.baseUrl}${url}`
    
    const header = {}
    if (app.globalData.token) {
      header['Authorization'] = `Bearer ${app.globalData.token}`
    }

    wx.uploadFile({
      url: fullUrl,
      filePath,
      name,
      formData,
      header,
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 200) {
            resolve(data)
          } else {
            reject(new Error(data.msg || '上传失败'))
          }
        } catch (e) {
          reject(new Error('解析响应失败'))
        }
      },
      fail: reject
    })
  })
}

module.exports = {
  request,
  get,
  post,
  put,
  del,
  upload
}
