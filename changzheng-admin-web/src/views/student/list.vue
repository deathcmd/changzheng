<template>
  <div class="student-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>学生信息管理</h2>
      <p class="sub-title">四川工商职业技术学院 · 智能制造与信息工程学院</p>
    </div>

    <!-- 操作栏 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <div class="filter-left">
          <el-input v-model="searchForm.keyword" placeholder="学号/姓名" clearable style="width: 200px" />
          <el-select v-model="searchForm.major" placeholder="专业" clearable style="width: 180px">
            <el-option v-for="m in majorList" :key="m" :label="m" :value="m" />
          </el-select>
          <el-select v-model="searchForm.className" placeholder="班级" clearable style="width: 180px">
            <el-option v-for="c in classList" :key="c" :label="c" :value="c" />
          </el-select>
          <el-select v-model="searchForm.isBound" placeholder="绑定状态" clearable style="width: 120px">
            <el-option label="已绑定" :value="1" />
            <el-option label="未绑定" :value="0" />
          </el-select>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <div class="filter-right">
          <el-radio-group v-model="viewMode" size="small" style="margin-right: 12px;">
            <el-radio-button value="list">列表</el-radio-button>
            <el-radio-button value="class">按班级</el-radio-button>
          </el-radio-group>
          <el-button type="success" @click="showImportDialog">
            <el-icon><Upload /></el-icon> 导入学生
          </el-button>
          <el-button @click="downloadTemplate">
            <el-icon><Download /></el-icon> 下载模板
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总人数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card bound">
          <div class="stat-value">{{ stats.bound }}</div>
          <div class="stat-label">已绑定</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card unbound">
          <div class="stat-value">{{ stats.unbound }}</div>
          <div class="stat-label">未绑定</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card rate">
          <div class="stat-value">{{ stats.bindRate }}%</div>
          <div class="stat-label">绑定率</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 学生列表 - 列表视图 -->
    <el-card v-if="viewMode === 'list'">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="studentNo" label="学号" width="140" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60" class-name="hide-on-mobile" />
        <el-table-column prop="major" label="专业" min-width="150" />
        <el-table-column prop="className" label="班级" width="180" />
        <el-table-column prop="grade" label="年级" width="100" class-name="hide-on-mobile" />
        <el-table-column prop="isBound" label="绑定状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isBound ? 'success' : 'info'">
              {{ row.isBound ? '已绑定' : '未绑定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="boundAt" label="绑定时间" width="170" class-name="hide-on-mobile" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" v-if="row.isBound" @click="handleUnbind(row)">解绑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 学生列表 - 班级分类视图 -->
    <div v-else class="class-view">
      <el-card v-for="classGroup in classGroupData" :key="classGroup.className" class="class-card">
        <template #header>
          <div class="class-header">
            <div class="class-info">
              <span class="class-name">{{ classGroup.className }}</span>
              <span class="class-major">{{ classGroup.major }}</span>
            </div>
            <div class="class-stats">
              <el-tag type="info" size="small">共 {{ classGroup.total }} 人</el-tag>
              <el-tag type="success" size="small">已认证 {{ classGroup.bound }}</el-tag>
              <el-tag type="warning" size="small">未认证 {{ classGroup.unbound }}</el-tag>
              <el-progress 
                :percentage="classGroup.bindRate" 
                :stroke-width="8"
                style="width: 100px; margin-left: 12px;"
              />
            </div>
          </div>
        </template>
        
        <el-table :data="classGroup.students" size="small" stripe>
          <el-table-column prop="studentNo" label="学号" width="130" />
          <el-table-column prop="name" label="姓名" width="90" />
          <el-table-column prop="gender" label="性别" width="60" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="isBound" label="认证状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isBound ? 'success' : 'info'" size="small">
                {{ row.isBound ? '已认证' : '未认证' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="boundAt" label="认证时间" min-width="160" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="warning" size="small" v-if="row.isBound" @click="handleUnbind(row)">解绑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入学生信息" width="500px">
      <div class="import-tips">
        <el-alert type="info" :closable="false">
          <template #title>
            <p>1. 请先下载导入模板，按格式填写学生信息</p>
            <p>2. 必填字段：学号、姓名</p>
            <p>3. 选填字段：性别、专业、班级、年级、手机号</p>
            <p>4. 重复学号将自动更新信息</p>
          </template>
        </el-alert>
      </div>
      
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将Excel文件拖到此处，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">只支持 .xlsx / .xls 文件</div>
        </template>
      </el-upload>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing">
          确认导入
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑学生信息" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="学号">
          <el-input v-model="editForm.studentNo" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="editForm.major" />
        </el-form-item>
        <el-form-item label="班级">
          <el-input v-model="editForm.className" />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="editForm.grade" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, Download, UploadFilled } from '@element-plus/icons-vue'
import * as studentApi from '@/api/student'

// Mock 开关（开发环境自动启用，生产环境自动禁用）
const USE_MOCK = import.meta.env.DEV

// 视图模式
const viewMode = ref('list')

// 搜索表单
const searchForm = reactive({
  keyword: '',
  major: '',
  className: '',
  isBound: null
})

// 专业和班级列表
const majorList = ref(['软件技术', '计算机应用技术', '大数据技术', '物联网应用技术', '人工智能技术应用'])
const classList = ref(['软件2301', '软件2302', '软件2303', '计应2301', '计应2302', '大数据2301', '物联网2301'])

// 所有数据（用于班级分组）
const allData = ref([])

// 统计数据
const stats = reactive({
  total: 0,
  bound: 0,
  unbound: 0,
  bindRate: 0
})

// 表格数据
const tableData = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 导入相关
const importDialogVisible = ref(false)
const uploadRef = ref(null)
const importFile = ref(null)
const importing = ref(false)

// 编辑相关
const editDialogVisible = ref(false)
const editForm = reactive({
  id: null,
  studentNo: '',
  name: '',
  gender: '',
  major: '',
  className: '',
  grade: '',
  phone: ''
})

// 班级分组数据
const classGroupData = computed(() => {
  const groups = {}
  
  // 过滤数据
  let filtered = allData.value.filter(item => {
    if (searchForm.keyword && !item.studentNo.includes(searchForm.keyword) && !item.name.includes(searchForm.keyword)) {
      return false
    }
    if (searchForm.major && item.major !== searchForm.major) return false
    if (searchForm.className && item.className !== searchForm.className) return false
    if (searchForm.isBound !== null && item.isBound !== searchForm.isBound) return false
    return true
  })
  
  // 按班级分组
  filtered.forEach(student => {
    const key = student.className || '未分班'
    if (!groups[key]) {
      groups[key] = {
        className: key,
        major: student.major,
        students: [],
        total: 0,
        bound: 0,
        unbound: 0,
        bindRate: 0
      }
    }
    groups[key].students.push(student)
    groups[key].total++
    if (student.isBound) {
      groups[key].bound++
    } else {
      groups[key].unbound++
    }
  })
  
  // 计算绑定率
  Object.values(groups).forEach(group => {
    group.bindRate = group.total > 0 ? Math.round(group.bound / group.total * 100) : 0
  })
  
  // 按班级名排序
  return Object.values(groups).sort((a, b) => a.className.localeCompare(b.className))
})

// 生成Mock数据
const generateMockData = () => {
  const data = []
  const majors = ['软件技术', '计算机应用技术', '大数据技术', '物联网应用技术']
  const classes = ['软件2301', '软件2302', '计应2301', '大数据2301', '物联网2301']
  
  for (let i = 1; i <= 100; i++) {
    const isBound = Math.random() > 0.6
    data.push({
      id: i,
      studentNo: `2023${String(i).padStart(4, '0')}`,
      name: `学生${i}`,
      gender: Math.random() > 0.4 ? '男' : '女',
      major: majors[Math.floor(Math.random() * majors.length)],
      className: classes[Math.floor(Math.random() * classes.length)],
      grade: '2023级',
      enrollYear: 2023,
      phone: `138${String(Math.floor(Math.random() * 100000000)).padStart(8, '0')}`,
      isBound: isBound ? 1 : 0,
      boundAt: isBound ? '2024-09-01 10:30:00' : null,
      status: 1
    })
  }
  return data
}

// 加载数据
const loadData = async () => {
  loading.value = true
  
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300))
    const mockData = generateMockData()
    allData.value = mockData
    
    // 过滤
    let filtered = mockData.filter(item => {
      if (searchForm.keyword && !item.studentNo.includes(searchForm.keyword) && !item.name.includes(searchForm.keyword)) {
        return false
      }
      if (searchForm.major && item.major !== searchForm.major) return false
      if (searchForm.className && item.className !== searchForm.className) return false
      if (searchForm.isBound !== null && item.isBound !== searchForm.isBound) return false
      return true
    })
    
    // 统计
    stats.total = mockData.length
    stats.bound = mockData.filter(i => i.isBound).length
    stats.unbound = stats.total - stats.bound
    stats.bindRate = stats.total > 0 ? ((stats.bound / stats.total) * 100).toFixed(1) : 0
    
    // 分页
    pagination.total = filtered.length
    const start = (pagination.page - 1) * pagination.size
    tableData.value = filtered.slice(start, start + pagination.size)
    
    loading.value = false
    return
  }
  
  // 真实API调用
  try {
    const [listRes, statsRes] = await Promise.all([
      studentApi.getStudentList({
        page: pagination.page,
        size: pagination.size,
        keyword: searchForm.keyword,
        major: searchForm.major,
        className: searchForm.className,
        isBound: searchForm.isBound
      }),
      studentApi.getStudentStats()
    ])
    
    tableData.value = listRes.data.records
    allData.value = listRes.data.records
    pagination.total = listRes.data.total
    
    Object.assign(stats, statsRes.data)
  } catch (error) {
    console.error('加载数据失败', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.major = ''
  searchForm.className = ''
  searchForm.isBound = null
  handleSearch()
}

// 分页
const handleSizeChange = () => {
  pagination.page = 1
  loadData()
}

const handlePageChange = () => {
  loadData()
}

// 显示导入对话框
const showImportDialog = () => {
  importFile.value = null
  importDialogVisible.value = true
}

// 文件选择
const handleFileChange = (file) => {
  importFile.value = file.raw
}

// 下载模板
const downloadTemplate = () => {
  // 创建模板数据
  const templateData = '学号,姓名,性别,专业,班级,年级,手机号\n20230001,张三,男,软件技术,软件2301,2023级,13800000001\n20230002,李四,女,计算机应用技术,计应2301,2023级,13800000002'
  
  const blob = new Blob(['\ufeff' + templateData], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '学生信息导入模板.csv'
  a.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('模板下载成功，请使用Excel打开编辑')
}

// 导入
const handleImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择要导入的文件')
    return
  }
  
  importing.value = true
  
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 1500))
    ElMessage.success('导入成功！共导入 50 条学生信息')
    importDialogVisible.value = false
    importing.value = false
    loadData()
    return
  }
  
  // 真实API上传
  try {
    const res = await studentApi.importStudents(importFile.value)
    ElMessage.success(`导入成功！共导入 ${res.data.successCount} 条学生信息`)
    importDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('导入失败', error)
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

// 编辑
const handleEdit = (row) => {
  Object.assign(editForm, row)
  editDialogVisible.value = true
}

// 保存编辑
const handleSaveEdit = async () => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300))
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadData()
    return
  }
  
  try {
    await studentApi.updateStudent(editForm.id, editForm)
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.error('保存失败')
  }
}

// 解绑
const handleUnbind = async (row) => {
  await ElMessageBox.confirm(`确定要解除 ${row.name} 的绑定吗？解绑后该学生需要重新认证`, '解绑确认', {
    type: 'warning'
  })
  
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300))
    ElMessage.success('解绑成功')
    loadData()
    return
  }
  
  try {
    await studentApi.unbindStudent(row.id)
    ElMessage.success('解绑成功')
    loadData()
  } catch (error) {
    console.error('解绑失败', error)
    ElMessage.error('解绑失败')
  }
}

// 删除
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定要删除学生 ${row.name} 吗？`, '删除确认', {
    type: 'error'
  })
  
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300))
    ElMessage.success('删除成功')
    loadData()
    return
  }
  
  try {
    await studentApi.deleteStudent(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    console.error('删除失败', error)
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.student-list {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.sub-title {
  margin: 0;
  font-size: 14px;
  color: #C41E3A;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-right {
  display: flex;
  gap: 12px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.stat-card.bound .stat-value {
  color: #67C23A;
}

.stat-card.unbound .stat-value {
  color: #909399;
}

.stat-card.rate .stat-value {
  color: #409EFF;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.import-tips {
  margin-bottom: 20px;
}

.import-tips p {
  margin: 4px 0;
}

.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
}

/* 班级分类视图 */
.class-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.class-card {
  margin-bottom: 0;
}

.class-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.class-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.class-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.class-major {
  font-size: 13px;
  color: #909399;
}

.class-stats {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
