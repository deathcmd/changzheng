const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { join } = require('node:path')
const test = require('node:test')
const vm = require('node:vm')

function harness() {
  const requests = []
  const uploads = []
  const app = { globalData: { token: 'old', refreshToken: 'refresh' }, logout() {} }
  const wx = {
    request(options) { requests.push(options) },
    uploadFile(options) { uploads.push(options) },
    showToast() {}, showModal() {}, reLaunch() {},
    getStorageSync() {}, setStorageSync() {}
  }
  const context = {
    module: { exports: {} }, wx, getApp: () => app, console,
    require(name) {
      if (name === '../config/index') return { baseUrl: 'https://example.invalid', useMock: false }
      if (name === './mock') return {}
      throw new Error(`Unexpected dependency: ${name}`)
    }
  }
  vm.runInNewContext(readFileSync(join(__dirname, '../miniprogram/utils/request.js'), 'utf8'), context)
  return { api: context.module.exports, requests, uploads, app }
}

for (const data of [null, '', [], { unexpected: true }]) {
  test(`malformed HTTP success rejects instead of stranding the caller: ${JSON.stringify(data)}`, async () => {
    const { api, requests } = harness()
    const result = api.get('/api/content/nodes')
    const rejected = assert.rejects(result, /响应格式/)
    requests[0].success({ statusCode: 200, data })
    await rejected
  })
}

test('network failures without errMsg still settle the request', async () => {
  const { api, requests } = harness()
  const result = api.get('/api/content/nodes')
  const rejected = assert.rejects(result)
  requests[0].fail({ message: 'offline' })
  await rejected
})

test('GET parameters extend an existing query and omit nulls', async () => {
  const { api, requests } = harness()
  const result = api.get('/api/rank/total?page=2', { pageSize: 10, empty: null })
  assert.equal(requests[0].url, 'https://example.invalid/api/rank/total?page=2&pageSize=10')
  requests[0].success({ statusCode: 200, data: { code: 200 } })
  await result
})

test('concurrent expired requests share one refresh and retry with the new token', async () => {
  const { api, requests, app } = harness()
  const results = [api.get('/one'), api.get('/two')]
  requests[0].success({ statusCode: 401 })
  requests[1].success({ statusCode: 401 })
  assert.equal(requests.filter(r => r.url.endsWith('/auth/refresh')).length, 1)
  requests[2].success({ statusCode: 200, data: { code: 200, data: { accessToken: 'new' } } })
  await new Promise(resolve => setImmediate(resolve))
  for (const request of requests.slice(3)) {
    assert.equal(request.header.Authorization, 'Bearer new')
    request.success({ statusCode: 200, data: { code: 200 } })
  }
  await Promise.all(results)
  assert.equal(app.globalData.token, 'new')
})

test('upload errors preserve the server message', async () => {
  const { api, uploads } = harness()
  const result = api.upload('/api/content/file/upload/image', 'test.png')
  const rejected = assert.rejects(result, /文件过大/)
  uploads[0].success({ statusCode: 400, data: JSON.stringify({ code: 40000, message: '文件过大' }) })
  await rejected
})

test('upload cannot report success for an HTTP failure', async () => {
  const { api, uploads } = harness()
  const result = api.upload('/api/content/file/upload/image', 'test.png')
  const rejected = assert.rejects(result, /上传失败/)
  uploads[0].success({ statusCode: 500, data: JSON.stringify({ code: 200 }) })
  await rejected
})
