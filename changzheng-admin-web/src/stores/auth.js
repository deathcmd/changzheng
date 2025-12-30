import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getAdminInfo } from '@/api/auth'
import router from '@/router'

// Mock 模式（开发环境自动启用，生产环境自动禁用）
const USE_MOCK = import.meta.env.DEV

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const adminInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  async function loginAction(username, password) {
    // Mock 模式登录
    if (USE_MOCK) {
      await new Promise(resolve => setTimeout(resolve, 500))
      const mockToken = 'mock_admin_token_' + Date.now()
      const mockAdminInfo = {
        id: 1,
        username: username,
        nickname: '管理员',
        role: 'admin'
      }
      token.value = mockToken
      localStorage.setItem('token', mockToken)
      adminInfo.value = mockAdminInfo
      return { code: 200, data: { accessToken: mockToken, adminInfo: mockAdminInfo } }
    }
    
    try {
      const res = await login({ username, password })
      token.value = res.data.accessToken
      localStorage.setItem('token', token.value)
      adminInfo.value = res.data.adminInfo
      return res
    } catch (error) {
      throw error
    }
  }

  async function fetchAdminInfo() {
    if (!token.value) return
    
    if (USE_MOCK) {
      adminInfo.value = { id: 1, username: 'admin', nickname: '管理员', role: 'admin' }
      return
    }
    
    try {
      const res = await getAdminInfo()
      adminInfo.value = res.data
    } catch (error) {
      logout()
    }
  }

  function logout() {
    token.value = ''
    adminInfo.value = null
    localStorage.removeItem('token')
    router.push('/login')
  }

  return {
    token,
    adminInfo,
    isLoggedIn,
    loginAction,
    fetchAdminInfo,
    logout
  }
})
