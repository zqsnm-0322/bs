// 入住老人自助登记页面
const app = getApp()

Page({
  data: {
    formData: {
      realName: '',       // 真实姓名
      username: '',       // 用户名
      age: '',           // 年龄
      gender: '',        // 性别
      password: '',       // 密码
      confirmPassword: '' // 确认密码
    },
    genderOptions: ['男', '女'],
    genderIndex: -1,
    agreementChecked: false, // 是否同意协议
    submitting: false
  },

  // 输入真实姓名
  onRealNameInput(e) {
    this.setData({ 'formData.realName': e.detail.value })
  },

  // 输入用户名
  onUsernameInput(e) {
    this.setData({ 'formData.username': e.detail.value })
  },

  // 输入年龄
  onAgeInput(e) {
    this.setData({ 'formData.age': e.detail.value })
  },

  // 选择性别
  onGenderChange(e) {
    const index = e.detail.value
    this.setData({ 
      genderIndex: index,
      'formData.gender': this.data.genderOptions[index]
    })
  },

  // 输入密码
  onPasswordInput(e) {
    this.setData({ 'formData.password': e.detail.value })
  },

  // 输入确认密码
  onConfirmPasswordInput(e) {
    this.setData({ 'formData.confirmPassword': e.detail.value })
  },

  // 同意协议
  onAgreementChange(e) {
    this.setData({ agreementChecked: e.detail.value.length > 0 })
  },

  // 验证表单
  validateForm() {
    const { realName, username, age, gender, password, confirmPassword } = this.data.formData

    // 验证真实姓名
    if (!realName || realName.trim().length < 2) {
      wx.showToast({ title: '请输入真实姓名', icon: 'none' })
      return false
    }

    // 验证用户名
    if (!username || username.trim().length < 3) {
      wx.showToast({ title: '用户名至少3位', icon: 'none' })
      return false
    }

    if (username.trim().length > 20) {
      wx.showToast({ title: '用户名最多20位', icon: 'none' })
      return false
    }

    // 验证用户名格式（字母、数字、下划线、中文）
    const usernameReg = /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/
    if (!usernameReg.test(username)) {
      wx.showToast({ title: '用户名只能包含字母、数字、下划线和中文', icon: 'none' })
      return false
    }

    // 验证年龄
    if (!age || age <= 0 || age > 150) {
      wx.showToast({ title: '请输入有效的年龄', icon: 'none' })
      return false
    }

    // 验证性别
    if (!gender) {
      wx.showToast({ title: '请选择性别', icon: 'none' })
      return false
    }

    // 验证密码
    if (!password || password.length < 6) {
      wx.showToast({ title: '密码至少6位', icon: 'none' })
      return false
    }

    if (password.length > 20) {
      wx.showToast({ title: '密码最多20位', icon: 'none' })
      return false
    }

    // 验证确认密码
    if (password !== confirmPassword) {
      wx.showToast({ title: '两次密码输入不一致', icon: 'none' })
      return false
    }

    // 验证协议
    if (!this.data.agreementChecked) {
      wx.showToast({ title: '请阅读并同意用户协议', icon: 'none' })
      return false
    }

    return true
  },

  // 提交注册
  handleRegister() {
    // 验证表单
    if (!this.validateForm()) {
      return
    }

    this.setData({ submitting: true })

    const registerData = {
      name: this.data.formData.realName.trim(),
      username: this.data.formData.username.trim(),
      password: this.data.formData.password,
      phone: '', // 手机号为空
      gender: this.data.formData.gender,
      age: parseInt(this.data.formData.age),
      roomBedNo: '',
      healthStatus: '',
      careManager: '',
      careZone: '',
      status: 'RESIDENT',
      idCard: '',
      emergencyContactPhone: '',
      emergencyContactName: '',
      emergencyContactRelation: '',
      admissionType: '长期',
      healthLevel: '自理',
      requestSource: 'MINIPROGRAM'
    }

    // 调用注册接口
    app.request('/resident', 'POST', registerData)
      .then(res => {
        const residentId = res.residentId || '未知'
        
        wx.showModal({
          title: '注册成功',
          content: `您的入住编号是：${residentId}\n请妥善保管，可用于登录`,
          showCancel: false,
          success: () => {
            // 跳转到登录页面
            wx.redirectTo({
              url: '/pages/login/login?type=resident'
            })
          }
        })
      })
      .catch(err => {
        console.error('注册失败:', err)
        wx.showToast({
          title: err.message || '注册失败，请重试',
          icon: 'none',
          duration: 2000
        })
      })
      .finally(() => {
        this.setData({ submitting: false })
      })
  },

  // 查看用户协议
  viewAgreement() {
    wx.navigateTo({
      url: '/pages/agreement/user-agreement'
    })
  },

  // 查看隐私政策
  viewPrivacy() {
    wx.navigateTo({
      url: '/pages/agreement/privacy-policy'
    })
  },

  // 返回登录
  goToLogin() {
    wx.navigateBack()
  }
})
