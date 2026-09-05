import { beforeEach, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  onRequest: vi.fn(), onResponse: vi.fn(), logout: vi.fn(),
  confirm: vi.fn(), error: vi.fn(), push: vi.fn()
}))
vi.mock('axios', () => ({ default: { create: () => ({
  interceptors: { request: { use: mocks.onRequest }, response: { use: mocks.onResponse } }
}) } }))
vi.mock('element-plus', () => ({ ElMessage: { error: mocks.error }, ElMessageBox: { confirm: mocks.confirm } }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ token: 'test-token', logout: mocks.logout }) }))
vi.mock('@/router', () => ({ default: { push: mocks.push } }))

import '../src/utils/request'

const [onSuccess, onFailure] = mocks.onResponse.mock.calls[0]
const [onRequest] = mocks.onRequest.mock.calls[0]
beforeEach(() => { mocks.logout.mockClear(); mocks.push.mockClear(); mocks.confirm.mockReset() })

it('attaches the active bearer token', () => {
  expect(onRequest({ headers: {} }).headers.Authorization).toBe('Bearer test-token')
})

it('preserves successful business and binary responses', () => {
  const data = { code: 200, data: [1, 2] }
  expect(onSuccess({ config: {}, data })).toBe(data)
  const binary = { config: { responseType: 'blob' }, data: 'file' }
  expect(onSuccess(binary)).toBe(binary)
})

it('handles cancellation of the expired-session dialog', async () => {
  mocks.confirm.mockRejectedValue('cancel')
  await expect(onSuccess({ config: {}, data: { code: 40101, message: 'expired' } })).rejects.toThrow('expired')
  await new Promise(resolve => setImmediate(resolve))
  expect(mocks.logout).not.toHaveBeenCalled()
})

it('logs out when the expired-session dialog is confirmed', async () => {
  mocks.confirm.mockResolvedValue('confirm')
  await expect(onSuccess({ config: {}, data: { code: 40100 } })).rejects.toThrow()
  await new Promise(resolve => setImmediate(resolve))
  expect(mocks.logout).toHaveBeenCalledOnce()
})

it('delegates HTTP 401 navigation to logout without a duplicate push', async () => {
  const failure = { response: { status: 401 } }
  await expect(onFailure(failure)).rejects.toBe(failure)
  expect(mocks.logout).toHaveBeenCalledOnce()
  expect(mocks.push).not.toHaveBeenCalled()
})
