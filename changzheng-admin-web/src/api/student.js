import request from '@/utils/request'

// 获取学生列表
export function getStudentList(params) {
  return request({
    url: '/admin/students',
    method: 'get',
    params
  })
}

// 获取统计数据
export function getStudentStats() {
  return request({
    url: '/admin/students/stats',
    method: 'get'
  })
}

// 导入学生
export function importStudents(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/students/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 更新学生信息
export function updateStudent(id, data) {
  return request({
    url: `/admin/students/${id}`,
    method: 'put',
    data
  })
}

// 删除学生
export function deleteStudent(id) {
  return request({
    url: `/admin/students/${id}`,
    method: 'delete'
  })
}

// 解绑学生
export function unbindStudent(id) {
  return request({
    url: `/admin/students/${id}/unbind`,
    method: 'post'
  })
}

// 下载导入模板
export function downloadTemplate() {
  return request({
    url: '/admin/students/template',
    method: 'get',
    responseType: 'blob'
  })
}
