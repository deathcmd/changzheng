import request from '@/utils/request'

// 上传图片
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/content/file/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 上传音频
export function uploadAudio(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/content/file/upload/audio',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 上传视频
export function uploadVideo(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/content/file/upload/video',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 通用上传
export function uploadFile(file, type = 'image') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return request({
    url: '/content/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
