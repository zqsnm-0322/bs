const app = getApp()

Page({
  data: {
    userInfo: {},
    formData: {
      realName: '',
      phone: '',
      email: ''
    },
    submitting: false
  },

  onLoad() {
    this.loadUserProfile()
  },

  // 加载用户信息
  loadUserProfile() {
    wx.showLoading({ title: '加载中...' })
    
    app.request('/user/profile', 'GET').then(res => {
      wx.hideLoading()
      
      this.setData({
        userInfo: res,
        formData: {
          realName: res.realName || '',
          phone: res.phone || '',
          email: res.email || ''
        }
      })
    }).catch(err => {
      wx.hideLoading()
      console.error('加载个人信息失败:', err)
      wx.showToast({
        title: err.msg || '加载失败',
        icon: 'none'
      })
    })
  },

  // 输入处理
  onRealNameInput(e) {
    this.setData({
      'formData.realName': e.detail.value
    })
  },

  onPhoneInput(e) {
    this.setData({
      'formData.phone': e.detail.value
    })
  },

  onEmailInput(e) {
    this.setData({
      'formData.email': e.detail.value
    })
  },

  // 表单验证
  validateForm() {
    const { realName, phone, email } = this.data.formData

    if (!realName || !realName.trim()) {
      wx.showToast({ title: '请输入真实姓名', icon: 'none' })
      return false
    }

    if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号码', icon: 'none' })
      return false
    }

    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      wx.showToast({ title: '请输入正确的邮箱地址', icon: 'none' })
      return false
    }

    return true
  },

  // 提交保存
  onSubmit() {
    if (!this.validateForm()) {
      return
    }

    this.setData({ submitting: true })
    wx.showLoading({ title: '保存中...', mask: true })

    app.request('/user/profile', 'PUT', this.data.formData).then(res => {
      wx.hideLoading()
      this.setData({ submitting: false })

      // 更新全局用户信息
      const updatedUserInfo = {
        ...app.globalData.userInfo,
        ...res
      }
      app.globalData.userInfo = updatedUserInfo
      wx.setStorageSync('userInfo', updatedUserInfo)

      wx.showToast({
        title: '保存成功',
        icon: 'success',
        duration: 1500
      })

      // 延迟返回
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }).catch(err => {
      wx.hideLoading()
      this.setData({ submitting: false })

      const errorMsg = err.msg || err.message || '保存失败'
      wx.showToast({
        title: errorMsg,
        icon: 'none',
        duration: 2000
      })
    })
  },

  // 取消编辑
  onCancel() {
    wx.navigateBack()
  }
})
