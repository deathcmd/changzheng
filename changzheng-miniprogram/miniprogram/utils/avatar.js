const isHttpsUrl = value => typeof value === 'string' && /^https:\/\/[^\s]+$/.test(value)

const removeLocalAvatar = filePath => {
  if (!filePath || isHttpsUrl(filePath)) return
  wx.removeSavedFile({
    filePath,
    fail: () => {}
  })
}

/**
 * chooseAvatar returns a temporary path. Persist it inside the mini program
 * sandbox before putting it in Storage so it survives beyond the current page.
 */
const persistAvatarUrl = (candidate, previous = '') => {
  if (!candidate || candidate === previous) {
    return Promise.resolve(candidate || previous)
  }
  if (isHttpsUrl(candidate)) {
    removeLocalAvatar(previous)
    return Promise.resolve(candidate)
  }

  return new Promise((resolve, reject) => {
    wx.saveFile({
      tempFilePath: candidate,
      success: result => {
        removeLocalAvatar(previous)
        resolve(result.savedFilePath)
      },
      fail: () => reject(new Error('头像保存失败，请重新选择'))
    })
  })
}

module.exports = {
  isHttpsUrl,
  persistAvatarUrl,
  removeLocalAvatar
}
