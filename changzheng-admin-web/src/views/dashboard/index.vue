<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">总用户数</p>
              <p class="stat-value">{{ stats.totalUsers || 0 }}</p>
            </div>
            <el-icon class="stat-icon" :size="48"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">今日活跃</p>
              <p class="stat-value">{{ stats.activeUsersToday || 0 }}</p>
            </div>
            <el-icon class="stat-icon today" :size="48"><UserFilled /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">平均里程</p>
              <p class="stat-value">{{ stats.averageMileage || 0 }} <small>km</small></p>
            </div>
            <el-icon class="stat-icon mileage" :size="48"><Odometer /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">完成率</p>
              <p class="stat-value">{{ (stats.completionRate * 100 || 0).toFixed(1) }} <small>%</small></p>
            </div>
            <el-icon class="stat-icon completion" :size="48"><TrophyBase /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>每日活跃用户趋势</span>
          </template>
          <div ref="activeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>节点访问Top10</span>
          </template>
          <div ref="nodeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

// Mock 开关（开发环境自动启用，生产环境自动禁用）
const USE_MOCK = import.meta.env.DEV

// Mock 数据
const mockDashboardData = {
  totalUsers: 1526,
  activeUsersToday: 328,
  averageMileage: 856.5,
  completionRate: 0.034,
  dailyActiveStats: [
    { date: '12-19', count: 280 },
    { date: '12-20', count: 310 },
    { date: '12-21', count: 295 },
    { date: '12-22', count: 340 },
    { date: '12-23', count: 380 },
    { date: '12-24', count: 320 },
    { date: '12-25', count: 328 }
  ],
  nodeClickStats: [
    { nodeName: '瑞金', viewCount: 1520 },
    { nodeName: '于都', viewCount: 1380 },
    { nodeName: '湘江', viewCount: 1250 },
    { nodeName: '遵义', viewCount: 980 },
    { nodeName: '赤水河', viewCount: 856 },
    { nodeName: '金沙江', viewCount: 720 },
    { nodeName: '泸定桥', viewCount: 650 },
    { nodeName: '夹金山', viewCount: 480 },
    { nodeName: '草地', viewCount: 320 },
    { nodeName: '会宁', viewCount: 180 }
  ]
}

const stats = reactive({
  totalUsers: 0,
  activeUsersToday: 0,
  averageMileage: 0,
  completionRate: 0,
  nodeClickStats: [],
  dailyActiveStats: []
})

const activeChartRef = ref(null)
const nodeChartRef = ref(null)
let activeChart = null
let nodeChart = null

onMounted(async () => {
  await fetchDashboardData()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  activeChart?.dispose()
  nodeChart?.dispose()
})

async function fetchDashboardData() {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300))
    Object.assign(stats, mockDashboardData)
    return
  }
  
  try {
    const { default: request } = await import('@/utils/request')
    const res = await request.get('/admin/stats/dashboard')
    Object.assign(stats, res.data)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

function initCharts() {
  // 活跃用户趋势图
  activeChart = echarts.init(activeChartRef.value)
  activeChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: stats.dailyActiveStats?.map(item => item.date) || []
    },
    yAxis: { type: 'value' },
    series: [{
      name: '活跃用户',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: stats.dailyActiveStats?.map(item => item.count) || []
    }]
  })

  // 节点访问柱状图
  nodeChart = echarts.init(nodeChartRef.value)
  nodeChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: stats.nodeClickStats?.map(item => item.nodeName) || [],
      axisLabel: { rotate: 45 }
    },
    yAxis: { type: 'value' },
    series: [{
      name: '访问次数',
      type: 'bar',
      data: stats.nodeClickStats?.map(item => item.viewCount) || [],
      itemStyle: { color: '#409EFF' }
    }]
  })
}

function handleResize() {
  activeChart?.resize()
  nodeChart?.resize()
}
</script>

<style lang="scss" scoped>
.dashboard {
  .stats-row {
    margin-bottom: 20px;
  }

  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .stat-info {
      .stat-label {
        color: #909399;
        font-size: 14px;
        margin: 0 0 8px;
      }

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
        margin: 0;

        small {
          font-size: 14px;
          font-weight: normal;
        }
      }
    }

    .stat-icon {
      color: #409EFF;
      
      &.today { color: #67C23A; }
      &.mileage { color: #E6A23C; }
      &.completion { color: #F56C6C; }
    }
  }

  .chart-row {
    .chart-container {
      height: 350px;
    }
  }
}
</style>
