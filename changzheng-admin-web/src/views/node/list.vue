<template>
  <div class="node-list">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>节点管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增节点
          </el-button>
        </div>
      </template>

      <el-table :data="nodeList" v-loading="loading" stripe>
        <el-table-column prop="sortOrder" label="序号" width="80" />
        <el-table-column prop="nodeCode" label="编码" width="120" />
        <el-table-column prop="nodeName" label="名称" width="120" />
        <el-table-column prop="mileageThreshold" label="里程阈值(km)" width="130" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="goContent(row)">内容</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑节点' : '新增节点'"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="节点编码" prop="nodeCode">
          <el-input v-model="form.nodeCode" placeholder="如: ZUNYI" />
        </el-form-item>
        <el-form-item label="节点名称" prop="nodeName">
          <el-input v-model="form.nodeName" placeholder="如: 遵义" />
        </el-form-item>
        <el-form-item label="里程阈值" prop="mileageThreshold">
          <el-input-number v-model="form.mileageThreshold" :min="0" :max="30000" />
          <span class="unit">公里</span>
        </el-form-item>
        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="form.longitude" :precision="6" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="form.latitude" :precision="6" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

// Mock 数据
const USE_MOCK = true
const mockNodeList = [
  { id: 1, nodeCode: 'RUIJIN', nodeName: '瑞金', mileageThreshold: 0, sortOrder: 1, description: '长征出发地', status: 1, longitude: 116.0279, latitude: 25.8847 },
  { id: 2, nodeCode: 'YUDU', nodeName: '于都', mileageThreshold: 60, sortOrder: 2, description: '红军集结出发地', status: 1, longitude: 115.4153, latitude: 25.9522 },
  { id: 3, nodeCode: 'XIANGJIANG', nodeName: '湘江', mileageThreshold: 400, sortOrder: 3, description: '湘江战役', status: 1, longitude: 111.0067, latitude: 25.2744 },
  { id: 4, nodeCode: 'ZUNYI', nodeName: '遵义', mileageThreshold: 1200, sortOrder: 4, description: '遵义会议', status: 1, longitude: 106.9271, latitude: 27.7256 },
  { id: 5, nodeCode: 'CHISHUI', nodeName: '赤水河', mileageThreshold: 1500, sortOrder: 5, description: '四渡赤水', status: 1, longitude: 105.7000, latitude: 28.0167 },
  { id: 6, nodeCode: 'JINSHAJIANG', nodeName: '金沙江', mileageThreshold: 2500, sortOrder: 6, description: '巧渡金沙江', status: 1, longitude: 103.2667, latitude: 26.6167 },
  { id: 7, nodeCode: 'LUDING', nodeName: '泸定桥', mileageThreshold: 3500, sortOrder: 7, description: '飞夺泸定桥', status: 1, longitude: 102.2333, latitude: 29.9167 },
  { id: 8, nodeCode: 'JIAJINSHAN', nodeName: '夹金山', mileageThreshold: 4000, sortOrder: 8, description: '翠越夹金山', status: 1, longitude: 102.8167, latitude: 30.8667 },
  { id: 9, nodeCode: 'CAODI', nodeName: '草地', mileageThreshold: 5000, sortOrder: 9, description: '过草地', status: 1, longitude: 102.9667, latitude: 33.4333 },
  { id: 10, nodeCode: 'HUINING', nodeName: '会宁', mileageThreshold: 25000, sortOrder: 10, description: '三大主力会师', status: 1, longitude: 105.0536, latitude: 35.6928 }
]

const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const nodeList = ref([])

const formRef = ref(null)
const form = reactive({
  id: null,
  nodeCode: '',
  nodeName: '',
  mileageThreshold: 0,
  sortOrder: 1,
  longitude: null,
  latitude: null,
  description: '',
  status: 1
})

const rules = {
  nodeCode: [{ required: true, message: '请输入节点编码', trigger: 'blur' }],
  nodeName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
  mileageThreshold: [{ required: true, message: '请输入里程阈值', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序序号', trigger: 'blur' }]
}

onMounted(() => {
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    if (USE_MOCK) {
      await new Promise(resolve => setTimeout(resolve, 300))
      nodeList.value = mockNodeList
      return
    }
    const { getNodeList } = await import('@/api/node')
    const res = await getNodeList()
    nodeList.value = res.data.list || res.data
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

function goContent(row) {
  router.push(`/nodes/${row.id}/content`)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除节点“${row.nodeName}”?`, '提示', {
      type: 'warning'
    })
    if (USE_MOCK) {
      nodeList.value = nodeList.value.filter(n => n.id !== row.id)
      ElMessage.success('删除成功')
      return
    }
    const { deleteNode } = await import('@/api/node')
    await deleteNode(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    if (USE_MOCK) {
      if (isEdit.value) {
        const idx = nodeList.value.findIndex(n => n.id === form.id)
        if (idx > -1) nodeList.value[idx] = { ...form }
        ElMessage.success('更新成功')
      } else {
        nodeList.value.push({ ...form, id: Date.now() })
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      return
    }

    const { createNode, updateNode } = await import('@/api/node')
    if (isEdit.value) {
      await updateNode(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createNode(form)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    fetchList()
  } finally {
    submitLoading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    nodeCode: '',
    nodeName: '',
    mileageThreshold: 0,
    sortOrder: 1,
    longitude: null,
    latitude: null,
    description: '',
    status: 1
  })
}
</script>

<style lang="scss" scoped>
.node-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .unit {
    margin-left: 8px;
    color: #909399;
  }
}
</style>
