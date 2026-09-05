import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    window.testUnhandledRejections = []
    window.addEventListener('unhandledrejection', event => {
      window.testUnhandledRejections.push(String(event.reason))
    })
  })
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname
    let data
    if (path === '/api/admin/login') {
      expect(route.request().postDataJSON()).toEqual({ username: 'test-admin', password: 'test-password' })
      data = { accessToken: 'e2e-test-token', adminInfo: { id: 1, realName: '测试管理员' } }
    } else {
      expect(route.request().headers().authorization).toBe('Bearer e2e-test-token')
      const responses = {
        '/api/admin/info': { id: 1, realName: '测试管理员' },
        '/api/admin/stats/dashboard': { dailyActiveStats: [], nodeClickStats: [] },
        '/api/admin/nodes/7': { id: 7, nodeName: '测试节点', mileageThreshold: 0 },
        '/api/admin/nodes/7/contents': [],
        '/api/admin/students': { list: [], total: 0 },
        '/api/admin/students/stats': {}
      }
      expect(Object.hasOwn(responses, path), `Unexpected API request: ${path}`).toBe(true)
      data = responses[path]
    }
    await route.fulfill({ json: { code: 200, data } })
  })
})

async function login(page, path) {
  await page.goto(`/admin${path}`)
  await expect(page).toHaveURL(/\/admin\/login\?redirect=/)
  await page.getByPlaceholder('请输入用户名').fill('test-admin')
  await page.getByPlaceholder('请输入密码').fill('test-password')
  await page.getByRole('button', { name: '登 录' }).click()
  await expect(page).toHaveURL(`/admin${path}`)
}

test('login preserves a nested destination and sidebar links remain absolute', async ({ page }) => {
  await login(page, '/nodes/7/content')
  await expect(page.getByRole('menuitem', { name: '节点管理' })).toHaveClass(/is-active/)
  await page.getByRole('menuitem', { name: '学生管理' }).click()
  await expect(page).toHaveURL('/admin/students')
  await expect(page.getByRole('menuitem', { name: '学生管理' })).toHaveClass(/is-active/)
  expect(await page.evaluate(() => window.testUnhandledRejections)).toEqual([])
})

test('canceling logout stays signed in; confirming clears the session', async ({ page }) => {
  await login(page, '/dashboard')
  await page.locator('.user-info').click()
  await page.getByText('退出登录', { exact: true }).click()
  await page.getByRole('button', { name: '取消', exact: true }).click()
  await expect(page.getByRole('dialog')).toBeHidden()
  await expect(page).toHaveURL('/admin/dashboard')
  expect(await page.evaluate(() => window.testUnhandledRejections)).toEqual([])
  await page.locator('.user-info').click()
  await page.getByText('退出登录', { exact: true }).click()
  await page.getByRole('button', { name: '确认', exact: true }).click()
  await expect(page).toHaveURL('/admin/login')
  expect(await page.evaluate(() => localStorage.getItem('token'))).toBeNull()
})
