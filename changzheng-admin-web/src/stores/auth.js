import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getAdminInfo } from '@/api/auth'
import router from '@/router'

// Mock 登录必须显式启用，避免开发环境无意绕过真实鉴权联调。
const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

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
    
    const res = await login({ username, password })
    token.value = res.data.accessToken
    localStorage.setItem('token', token.value)
    adminInfo.value = res.data.adminInfo
    return res
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
