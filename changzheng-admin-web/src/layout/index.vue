<template>
  <el-container class="layout-container" :class="{ 'is-mobile': isMobile }">
    <!-- 侧边栏 (PC端) -->
    <el-aside v-if="!isMobile" :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span class="logo-icon" v-if="!isCollapse">🚩</span>
        <span v-if="!isCollapse">云上重走长征路</span>
        <span class="logo-icon" v-else>🚩</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        :router="true"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-menu-item :index="route.path" v-if="!route.meta?.hidden">
            <el-icon><component :is="route.meta?.icon" /></el-icon>
            <template #title>{{ route.meta?.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 侧边栏 (移动端抽屉) -->
    <el-drawer
      v-else
      v-model="drawerVisible"
      direction="ltr"
      size="220px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="sidebar mobile-sidebar">
        <div class="logo">
          <span class="logo-icon">🚩</span>
          <span>云上重走长征路</span>
        </div>
        <el-menu
          :default-active="$route.path"
          :router="true"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          @select="drawerVisible = false"
        >
          <template v-for="route in menuRoutes" :key="route.path">
            <el-menu-item :index="route.path" v-if="!route.meta?.hidden">
              <el-icon><component :is="route.meta?.icon" /></el-icon>
              <template #title>{{ route.meta?.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </div>
    </el-drawer>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="handleToggleSidebar">
            <Fold v-if="!isCollapse && !isMobile" />
            <Expand v-else-if="isCollapse && !isMobile" />
            <Menu v-else />
          </el-icon>
          <el-breadcrumb separator="/" v-if="!isMobile">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta?.title }}</el-breadcrumb-item>
          </el-breadcrumb>
          <span v-else class="mobile-title">{{ $route.meta?.title }}</span>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" icon="User" />
              <span class="username" v-if="!isMobile">{{ authStore.adminInfo?.realName || '管理员' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'
import { Menu, Fold, Expand, User, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const isCollapse = ref(false)
const isMobile = ref(false)
const drawerVisible = ref(false)

const menuRoutes = computed(() => {
  const mainRoute = router.options.routes.find(r => r.path === '/')
  return mainRoute?.children || []
})

const checkMobile = () => {
  const rect = document.body.getBoundingClientRect()
  isMobile.value = rect.width < 768
  if (isMobile.value) {
    isCollapse.value = true
  }
}

const handleToggleSidebar = () => {
  if (isMobile.value) {
    drawerVisible.value = !drawerVisible.value
  } else {
    isCollapse.value = !isCollapse.value
  }
}

onMounted(() => {
  authStore.fetchAdminInfo()
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

function handleLogout() {
  ElMessageBox.confirm('确认退出登录?', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    authStore.logout()
  })
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
  height: 100%;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    
    .logo-icon {
      font-size: 24px;
      margin-right: 8px;
    }
  }

  .el-menu {
    border-right: none;
  }
}

.mobile-sidebar {
  width: 100% !important;
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;

  .header-left {
    display: flex;
    align-items: center;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      margin-right: 20px;
    }

    .mobile-title {
      font-size: 16px;
      font-weight: bold;
      color: #333;
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;

      .username {
        margin-left: 8px;
        color: #333;
      }
    }
  }
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}

/* 适配移动端内容边距 */
.is-mobile {
  .main-content {
    padding: 10px;
  }
  
  .header {
    padding: 0 10px;
  }
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

:deep(.mobile-drawer) {
  .el-drawer__body {
    padding: 0;
    background-color: #304156;
  }
}
</style>
