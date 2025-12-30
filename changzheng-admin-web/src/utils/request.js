import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers['Authorization'] = `Bearer ${authStore.token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    
    // 业务状态码判断
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      
      // Token过期
      if (res.code === 40100 || res.code === 40101) {
        ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          const authStore = useAuthStore()
          authStore.logout()
        })
      }
      
      return Promise.reject(new Error(res.message || 'Error'))
    }
    
    return res
  },
  error => {
    console.error('响应错误:', error)
    
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const authStore = useAuthStore()
        authStore.logout()
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('没有操作权限')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status >= 500) {
        ElMessage.error('服务器内部错误')
      }
    } else if (error.message.includes('timeout')) {
      ElMessage.error('请求超时，请重试')
    } else {
      ElMessage.error('网络连接异常')
    }
    
    return Promise.reject(error)
  }
)

export default request
