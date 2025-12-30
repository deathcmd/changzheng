// utils/util.js - 工具函数

/**
 * 格式化日期
 * @param {Date|string|number} date 日期
 * @param {string} format 格式 默认 YYYY-MM-DD
 */
const formatDate = (date, format = 'YYYY-MM-DD') => {
  if (!date) return ''
  
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化数字（千分位）
 * @param {number} num 数字
 */
const formatNumber = (num) => {
  if (num === null || num === undefined) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 格式化里程
 * @param {number} km 公里数
 */
const formatMileage = (km) => {
  if (!km || km <= 0) return '0'
  if (km < 1) return (km * 1000).toFixed(0) + 'm'
  if (km < 100) return km.toFixed(2) + 'km'
  return formatNumber(km.toFixed(1)) + 'km'
}

/**
 * 格式化步数
 * @param {number} steps 步数
 */
const formatSteps = (steps) => {
  if (!steps || steps <= 0) return '0'
  if (steps >= 10000) {
    return (steps / 10000).toFixed(1) + '万'
  }
  return formatNumber(steps)
}

/**
 * 计算里程百分比
 * @param {number} current 当前里程
 * @param {number} total 总里程
 */
const calculateProgress = (current, total) => {
  if (!total || total <= 0) return 0
  const percent = (current / total) * 100
  return Math.min(percent, 100).toFixed(2)
}

/**
 * 获取相对时间
 * @param {Date|string|number} date 日期
 */
const getRelativeTime = (date) => {
  if (!date) return ''
  
  const d = new Date(date)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  
  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前'
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前'
  } else if (diff < 7 * day) {
    return Math.floor(diff / day) + '天前'
  } else {
    return formatDate(date)
  }
}

/**
 * 防抖函数
 * @param {Function} fn 函数
 * @param {number} delay 延迟毫秒
 */
const debounce = (fn, delay = 300) => {
  let timer = null
  return function(...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 * @param {Function} fn 函数
 * @param {number} interval 间隔毫秒
 */
const throttle = (fn, interval = 300) => {
  let lastTime = 0
  return function(...args) {
    const now = Date.now()
    if (now - lastTime >= interval) {
      lastTime = now
      fn.apply(this, args)
    }
  }
}

/**
 * 深拷贝
 * @param {any} obj 对象
 */
const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj)
  if (obj instanceof Array) return obj.map(item => deepClone(item))
  
  const result = {}
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      result[key] = deepClone(obj[key])
    }
  }
  return result
}

/**
 * 生成唯一ID
 */
const generateId = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

/**
 * 判断是否为空
 * @param {any} value 值
 */
const isEmpty = (value) => {
  if (value === null || value === undefined) return true
  if (typeof value === 'string') return value.trim() === ''
  if (Array.isArray(value)) return value.length === 0
  if (typeof value === 'object') return Object.keys(value).length === 0
  return false
}

/**
 * 手机号脱敏
 * @param {string} phone 手机号
 */
const maskPhone = (phone) => {
  if (!phone || phone.length < 11) return phone
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/**
 * 学号脱敏
 * @param {string} studentId 学号
 */
const maskStudentId = (studentId) => {
  if (!studentId || studentId.length < 6) return studentId
  const start = studentId.substring(0, 2)
  const end = studentId.substring(studentId.length - 2)
  return start + '****' + end
}

/**
 * 获取系统信息
 */
const getSystemInfo = () => {
  try {
    return wx.getSystemInfoSync()
  } catch (e) {
    console.error('获取系统信息失败', e)
    return {}
  }
}

/**
 * 检查网络状态
 */
const checkNetwork = () => {
  return new Promise((resolve) => {
    wx.getNetworkType({
      success: (res) => {
        resolve({
          isConnected: res.networkType !== 'none',
          networkType: res.networkType
        })
      },
      fail: () => {
        resolve({
          isConnected: false,
          networkType: 'none'
        })
      }
    })
  })
}

module.exports = {
  formatDate,
  formatNumber,
  formatMileage,
  formatSteps,
  calculateProgress,
  getRelativeTime,
  debounce,
  throttle,
  deepClone,
  generateId,
  isEmpty,
  maskPhone,
  maskStudentId,
  getSystemInfo,
  checkNetwork
}
