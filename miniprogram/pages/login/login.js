const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    userType: 'CAREGIVER', // CAREGIVER/CARE_MANAGER: 员工, RESIDENT: 入住老人
    loading: false
  },

  onLoad(options) {
    // 支持从URL参数指定用户类型
    if (options.type) {
      this.setData({ userType: options.type.toUpperCase() })
    }
  },

  // 切换用户类型
  switchUserType(e) {
    const userType = e.currentTarget.dataset.type
    this.setData({
      userType,
      username: '',
      password: ''
    })
  },

  onInputUser(e) {
    this.setData({ username: e.detail.value })
  },

  onInputPwd(e) {
    this.setData({ password: e.detail.value })
  },

  handleLogin() {
    // 验证输入
    if (!this.data.username || !this.data.password) {
      return wx.showToast({ title: '请输入账号密码', icon: 'none' })
    }

    if (this.data.username.length < 2) {
      return wx.showToast({ title: '账号格式不正确', icon: 'none' })
    }

    if (this.data.password.length < 6) {
      return wx.showToast({ title: '密码至少6位', icon: 'none' })
    }

    this.setData({ loading: true })

    const loginData = {
      username: this.data.username,
      password: this.data.password,
      userType: this.data.userType
    }

    // 调用后端登录接口
    app.request('/login', 'POST', loginData).then(res => {
      // 登录成功，保存用户信息
      wx.setStorageSync('token', res.token)
      const userInfo = app.normalizeUser(res.userInfo)
      wx.setStorageSync('userInfo', userInfo)
      app.globalData.userInfo = userInfo

      wx.showToast({
        title: '登录成功',
        icon: 'success'
      })

      // 根据用户类型跳转到不同页面
      setTimeout(() => {
        if (this.data.userType === 'RESIDENT' || res.userInfo.role === 'RESIDENT') {
          // 入住老人跳转到首页
          wx.switchTab({
            url: '/pages/index/index'
          })
        } else {
          // 医护人员跳转到首页
          wx.switchTab({
            url: '/pages/index/index'
          })
        }
      }, 1500)
    }).catch(err => {
      console.error('登录失败:', err)
      wx.showToast({
        title: err.msg || err.message || '登录失败，请检查账号密码',
        icon: 'none',
        duration: 2000
      })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  // 快速登录（演示用）
  quickLogin(e) {
    const type = e.currentTarget.dataset.type

    let username, password, userType
    if (type === 'caregiver') {
      username = 'caregiver001'
      password = '123456'
      userType = 'CAREGIVER'
    } else if (type === 'careManager') {
      username = 'care_manager01'
      password = '123456'
      userType = 'CARE_MANAGER'
    } else if (type === 'resident') {
      username = 'resident001'
      password = '123456'
      userType = 'RESIDENT'
    } else {
      return wx.showToast({ title: '未知登录类型', icon: 'none' })
    }

    this.setData({ username, password, userType })

    const typeNames = { caregiver: '护理员', careManager: '照护主管', resident: '入住老人' }
    wx.showModal({
      title: '快速登录',
      content: `使用${typeNames[type] || type}演示账号登录？`,
      success: (res) => {
        if (res.confirm) {
          // 确保账号和角色状态写入完成后再发起请求，避免使用上一次选择的角色。
          this.setData({ username, password, userType }, () => this.handleLogin())
        }
      }
    })
  },

  // 前往注册页面
  goToRegister() {
    wx.navigateTo({
      url: '/pages/register/register'
    })
  }
})
