<template>
  <div class="node-content">
    <!-- 返回和标题 -->
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <h2>{{ nodeInfo.nodeName }} - 内容管理</h2>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon> 添加内容
      </el-button>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 20px">
      <template #title>
        当用户里程达到 <strong>{{ nodeInfo.mileageThreshold }} 公里</strong> 时，将解锁此节点并自动弹出以下内容供学习
      </template>
    </el-alert>

    <!-- 内容列表 -->
    <el-table :data="contentList" v-loading="loading" stripe>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.contentType)">
            <el-icon style="vertical-align: middle; margin-right: 4px">
              <component :is="getTypeIcon(row.contentType)" />
            </el-icon>
            {{ getTypeName(row.contentType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="duration" label="时长/字数" width="120" />
      <el-table-column label="自动播放" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.autoPlay" @change="updateContent(row)" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="updateContent(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="previewContent(row)">预览</el-button>
          <el-button size="small" @click="editContent(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteContent(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑内容' : '添加内容'" width="700px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="内容类型" prop="contentType">
          <el-radio-group v-model="form.contentType">
            <el-radio-button value="video">
              <el-icon><VideoCamera /></el-icon> 视频
            </el-radio-button>
            <el-radio-button value="audio">
              <el-icon><Headset /></el-icon> 音频
            </el-radio-button>
            <el-radio-button value="article">
              <el-icon><Document /></el-icon> 文章
            </el-radio-button>
            <el-radio-button value="image">
              <el-icon><Picture /></el-icon> 图片
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入内容标题" />
        </el-form-item>

        <el-form-item label="时长/字数" prop="duration">
          <el-input v-model="form.duration" placeholder="如：5分钟、1500字" style="width: 200px" />
        </el-form-item>

        <!-- 视频上传 -->
        <el-form-item v-if="form.contentType === 'video'" label="上传视频" prop="mediaUrl">
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              :before-upload="(file) => handleUpload(file, 'video')"
              accept=".mp4,.avi,.mov,.wmv,.flv,.mkv,.webm"
            >
              <el-button type="primary" :loading="uploading">选择视频文件</el-button>
            </el-upload>
            <el-input v-model="form.mediaUrl" placeholder="或输入视频链接" style="margin-top: 10px">
              <template #prepend>URL</template>
            </el-input>
            <div v-if="form.mediaUrl" class="upload-preview">
              <video :src="form.mediaUrl" controls style="max-width: 300px; max-height: 200px"></video>
            </div>
          </div>
          <div class="form-tip">支持 mp4, avi, mov, wmv, flv, mkv, webm 格式，最大100MB</div>
        </el-form-item>

        <!-- 音频上传 -->
        <el-form-item v-if="form.contentType === 'audio'" label="上传音频" prop="mediaUrl">
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              :before-upload="(file) => handleUpload(file, 'audio')"
              accept=".mp3,.wav,.ogg,.m4a,.flac,.aac"
            >
              <el-button type="primary" :loading="uploading">选择音频文件</el-button>
            </el-upload>
            <el-input v-model="form.mediaUrl" placeholder="或输入音频链接" style="margin-top: 10px">
              <template #prepend>URL</template>
            </el-input>
            <div v-if="form.mediaUrl" class="upload-preview">
              <audio :src="form.mediaUrl" controls></audio>
            </div>
          </div>
          <div class="form-tip">支持 mp3, wav, ogg, m4a, flac, aac 格式，最大100MB</div>
        </el-form-item>

        <!-- 图片上传 -->
        <el-form-item v-if="form.contentType === 'image'" label="上传图片" prop="mediaUrl">
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              :before-upload="(file) => handleUpload(file, 'image')"
              accept=".jpg,.jpeg,.png,.gif,.webp,.bmp"
            >
              <el-button type="primary" :loading="uploading">选择图片文件</el-button>
            </el-upload>
            <el-input v-model="form.mediaUrl" placeholder="或输入图片链接" style="margin-top: 10px">
              <template #prepend>URL</template>
            </el-input>
            <div v-if="form.mediaUrl" class="upload-preview">
              <img :src="form.mediaUrl" style="max-width: 300px; max-height: 200px" />
            </div>
          </div>
          <div class="form-tip">支持 jpg, jpeg, png, gif, webp, bmp 格式，最大100MB</div>
        </el-form-item>

        <!-- 文章内容 -->
        <el-form-item v-if="form.contentType === 'article'" label="文章内容" prop="content">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="10" 
            placeholder="请输入文章内容，支持HTML格式"
          />
        </el-form-item>

        <el-form-item label="封面图" prop="coverUrl">
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              :before-upload="handleCoverUpload"
              accept=".jpg,.jpeg,.png,.gif,.webp"
            >
              <el-button :loading="coverUploading">上传封面</el-button>
            </el-upload>
            <el-input v-model="form.coverUrl" placeholder="或输入封面图URL" style="margin-top: 10px">
              <template #prepend>URL</template>
            </el-input>
            <div v-if="form.coverUrl" class="upload-preview">
              <img :src="form.coverUrl" style="max-width: 150px; max-height: 100px" />
            </div>
          </div>
        </el-form-item>

        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="1" :max="100" />
        </el-form-item>

        <el-form-item label="自动播放">
          <el-switch v-model="form.autoPlay" />
          <span class="form-tip" style="margin-left: 10px">开启后，用户解锁节点时自动弹出此内容</span>
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewVisible" :title="previewData.title" width="800px" destroy-on-close>
      <!-- 视频预览 -->
      <div v-if="previewData.contentType === 'video'" class="preview-container">
        <video :src="previewData.mediaUrl" controls style="width: 100%; max-height: 450px"></video>
      </div>
      <!-- 音频预览 -->
      <div v-else-if="previewData.contentType === 'audio'" class="preview-container audio">
        <img :src="previewData.coverUrl || '/default-audio-cover.png'" class="audio-cover" />
        <audio :src="previewData.mediaUrl" controls style="width: 100%; margin-top: 20px"></audio>
      </div>
      <!-- 文章预览 -->
      <div v-else-if="previewData.contentType === 'article'" class="preview-container article">
        <div v-html="previewData.content"></div>
      </div>
      <!-- 图片预览 -->
      <div v-else-if="previewData.contentType === 'image'" class="preview-container">
        <img :src="previewData.mediaUrl" style="max-width: 100%" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, VideoCamera, Headset, Document, Picture } from '@element-plus/icons-vue'
import { uploadImage, uploadAudio, uploadVideo } from '@/api/upload'

const route = useRoute()
const router = useRouter()
const nodeId = route.params.id

// 上传状态
const uploading = ref(false)
const coverUploading = ref(false)

// Mock 数据
const mockNodes = {
  1: { id: 1, nodeName: '瑞金', mileageThreshold: 0 },
  2: { id: 2, nodeName: '于都', mileageThreshold: 60 },
  3: { id: 3, nodeName: '湘江', mileageThreshold: 400 },
  4: { id: 4, nodeName: '遵义', mileageThreshold: 1200 },
  5: { id: 5, nodeName: '赤水河', mileageThreshold: 1500 },
  6: { id: 6, nodeName: '金沙江', mileageThreshold: 2500 },
  7: { id: 7, nodeName: '泸定桥', mileageThreshold: 3500 },
  8: { id: 8, nodeName: '夹金山', mileageThreshold: 4000 },
  9: { id: 9, nodeName: '草地', mileageThreshold: 5000 },
  10: { id: 10, nodeName: '会宁', mileageThreshold: 25000 }
}

const mockContents = [
  { id: 1, nodeId: 1, contentType: 'video', title: '长征出发：从瑞金说起', duration: '8分钟', mediaUrl: 'https://example.com/video1.mp4', coverUrl: '', sortOrder: 1, autoPlay: true, enabled: true },
  { id: 2, nodeId: 1, contentType: 'article', title: '瑞金：红色故都的历史', duration: '2000字', content: '<h3>瑞金简介</h3><p>瑞金位于江西省东南部，是著名的红色故都、共和国摇篮...</p>', coverUrl: '', sortOrder: 2, autoPlay: false, enabled: true },
  { id: 3, nodeId: 1, contentType: 'audio', title: '红军长征歌曲精选', duration: '15分钟', mediaUrl: 'https://example.com/audio1.mp3', coverUrl: '', sortOrder: 3, autoPlay: false, enabled: true }
]

const loading = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const nodeInfo = reactive({ id: nodeId, nodeName: '', mileageThreshold: 0 })
const contentList = ref([])
const previewData = reactive({})

const form = reactive({
  id: null,
  contentType: 'video',
  title: '',
  duration: '',
  mediaUrl: '',
  content: '',
  coverUrl: '',
  sortOrder: 1,
  autoPlay: true,
  enabled: true
})

const rules = {
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

onMounted(() => {
  loadNodeInfo()
  loadContents()
})

function loadNodeInfo() {
  const node = mockNodes[nodeId]
  if (node) {
    Object.assign(nodeInfo, node)
  }
}

function loadContents() {
  loading.value = true
  setTimeout(() => {
    contentList.value = mockContents.filter(c => c.nodeId == nodeId)
    loading.value = false
  }, 300)
}

function goBack() {
  router.push('/nodes')
}

function getTypeTag(type) {
  const map = { video: 'danger', audio: 'warning', article: '', image: 'success' }
  return map[type] || ''
}

function getTypeName(type) {
  const map = { video: '视频', audio: '音频', article: '文章', image: '图片' }
  return map[type] || type
}

function getTypeIcon(type) {
  const map = { video: VideoCamera, audio: Headset, article: Document, image: Picture }
  return map[type] || Document
}

function showAddDialog() {
  isEdit.value = false
  Object.assign(form, {
    id: null, contentType: 'video', title: '', duration: '', 
    mediaUrl: '', content: '', coverUrl: '', sortOrder: contentList.value.length + 1, 
    autoPlay: true, enabled: true
  })
  dialogVisible.value = true
}

function editContent(row) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

function previewContent(row) {
  Object.assign(previewData, row)
  previewVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  if (isEdit.value) {
    const idx = contentList.value.findIndex(c => c.id === form.id)
    if (idx > -1) contentList.value[idx] = { ...form }
    ElMessage.success('更新成功')
  } else {
    contentList.value.push({ ...form, id: Date.now(), nodeId: parseInt(nodeId) })
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
}

function updateContent(row) {
  ElMessage.success('更新成功')
}

function deleteContent(row) {
  ElMessageBox.confirm(`确定删除"${row.title}"吗？`, '提示', { type: 'warning' })
    .then(() => {
      contentList.value = contentList.value.filter(c => c.id !== row.id)
      ElMessage.success('删除成功')
    })
    .catch(() => {})
}

// 文件上传处理
async function handleUpload(file, type) {
  uploading.value = true
  try {
    let res
    if (type === 'video') {
      res = await uploadVideo(file)
    } else if (type === 'audio') {
      res = await uploadAudio(file)
    } else {
      res = await uploadImage(file)
    }
    
    if (res.code === 200 && res.data) {
      form.mediaUrl = res.data.url
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败，请检查网络')
  } finally {
    uploading.value = false
  }
  return false // 阻止默认上传行为
}

// 封面图上传
async function handleCoverUpload(file) {
  coverUploading.value = true
  try {
    const res = await uploadImage(file)
    if (res.code === 200 && res.data) {
      form.coverUrl = res.data.url
      ElMessage.success('封面上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error) {
    console.error('封面上传失败:', error)
    ElMessage.error('封面上传失败')
  } finally {
    coverUploading.value = false
  }
  return false
}
</script>

<style lang="scss" scoped>
.node-content {
  .page-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
    
    h2 {
      flex: 1;
      margin: 0;
      color: #303133;
    }
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }

  .upload-area {
    .upload-preview {
      margin-top: 10px;
      padding: 10px;
      background: #f5f7fa;
      border-radius: 4px;
      
      img, video {
        border-radius: 4px;
      }
    }
  }

  .preview-container {
    min-height: 200px;
    
    &.audio {
      text-align: center;
      padding: 40px;
      
      .audio-cover {
        width: 200px;
        height: 200px;
        border-radius: 8px;
        object-fit: cover;
      }
    }
    
    &.article {
      padding: 20px;
      line-height: 1.8;
      max-height: 500px;
      overflow-y: auto;
    }
  }
}
</style>
