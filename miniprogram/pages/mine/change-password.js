const app = getApp()

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    showOldPassword: false,
    showNewPassword: false,
    showConfirmPassword: false,
    passwordStrength: 0,
    strengthText: '',
    submitting: false
  },

  onOldPasswordInput(e) {
    this.setData({ oldPassword: e.detail.value })
  },

  onNewPasswordInput(e) {
    const password = e.detail.value
    this.setData({ newPassword: password })
    this.checkPasswordStrength(password)
  },

  onConfirmPasswordInput(e) {
    this.setData({ confirmPassword: e.detail.value })
  },

  toggleOldPassword() {
    this.setData({ showOldPassword: !this.data.showOldPassword })
  },

  toggleNewPassword() {
    this.setData({ showNewPassword: !this.data.showNewPassword })
  },

  toggleConfirmPassword() {
    this.setData({ showConfirmPassword: !this.data.showConfirmPassword })
  },

  // 检查密码强度
  checkPasswordStrength(password) {
    if (!password) {
      this.setData({ passwordStrength: 0, strengthText: '' })
      return
    }

    let strength = 0
    let strengthText = ''

    // 长度检查
    if (password.length >= 6) strength++
    if (password.length >= 10) strength++

    // 复杂度检查
    const hasLetter = /[a-zA-Z]/.test(password)
    const hasNumber = /[0-9]/.test(password)
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password)

    if (hasLetter && hasNumber) strength++
    if (hasSpecial) strength++

    // 限制最大强度为3
    strength = Math.min(strength, 3)

    if (strength === 1) {
      strengthText = '弱'
    } else if (strength === 2) {
      strengthText = '中'
    } else if (strength === 3) {
      strengthText = '强'
    }

    this.setData({ passwordStrength: strength, strengthText })
  },

  validateForm() {
    const { oldPassword, newPassword, confirmPassword } = this.data

    if (!oldPassword) {
      wx.showToast({ title: '请输入原密码', icon: 'none' })
      return false
    }

    if (!newPassword) {
      wx.showToast({ title: '请输入新密码', icon: 'none' })
      return false
    }

    if (newPassword.length < 6 || newPassword.length > 20) {
      wx.showToast({ title: '新密码长度为6-20位', icon: 'none' })
      return false
    }

    if (!confirmPassword) {
      wx.showToast({ title: '请确认新密码', icon: 'none' })
      return false
    }

    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次输入的密码不一致', icon: 'none' })
      return false
    }

    if (oldPassword === newPassword) {
      wx.showToast({ title: '新密码不能与原密码相同', icon: 'none' })
      return false
    }

    return true
  },

  onSubmit() {
    if (!this.validateForm()) {
      return
    }

    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}

    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中...', mask: true })

    const requestData = {
      oldPassword: this.data.oldPassword,
      newPassword: this.data.newPassword
    }

    // 使用新的统一接口
    app.request('/user/password', 'PUT', requestData).then(res => {
      wx.hideLoading()
      this.setData({ submitting: false })

      wx.showModal({
        title: '修改成功',
        content: '密码修改成功，请重新登录',
        showCancel: false,
        success: () => {
          // 清除登录信息
          wx.clearStorageSync()
          app.globalData.userInfo = null
          
          // 跳转到登录页
          wx.reLaunch({ url: '/pages/login/login' })
        }
      })
    }).catch(err => {
      wx.hideLoading()
      this.setData({ submitting: false })

      const errorMsg = err.msg || err.message || '修改失败'
      wx.showToast({
        title: errorMsg,
        icon: 'none',
        duration: 2000
      })
    })
  }
})
