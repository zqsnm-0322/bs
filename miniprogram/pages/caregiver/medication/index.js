const app = getApp()

Page({
  data: {
    residentList: [],
    selectedResident: null,
    selectedResidentIndex: -1,
    formData: {
      medicationName: '',
      dosage: '',
      unit: 'mg',
      frequency: '',
      route: '',
      startDate: '',
      endDate: '',
      duration: '',
      purpose: '',
      notes: ''
    },
    unitOptions: ['mg', 'g', 'ml', '片', '粒', '支'],
    unitIndex: 0,
    frequencyOptions: ['每日1次', '每日2次', '每日3次', '每日4次', '每8小时1次', '每12小时1次', '必要时'],
    frequencyIndex: 0,
    routeOptions: ['口服', '静脉注射', '静脉滴注', '肌肉注射', '皮下注射', '外用', '雾化吸入'],
    routeIndex: 0
  },

  onLoad() {
    // 权限检查：只有医护人员可以访问
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    if (userInfo.role === 'RESIDENT') {
      wx.showModal({
        title: '权限不足',
        content: '老人无法访问此页面',
        showCancel: false,
        success: () => {
          wx.switchTab({
            url: '/pages/index/index'
          })
        }
      })
      return
    }

    this.loadResidentList()
    this.initDefaultDate()
  },

  initDefaultDate() {
    const today = new Date()
    const dateStr = this.formatDate(today)
    this.setData({
      'formData.startDate': dateStr
    })
  },

  formatDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  loadResidentList() {
    wx.showLoading({ title: '加载老人列表...' })
    
    // 获取当前用户信息
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const token = wx.getStorageSync('token') || ''
    
    // 调试信息
    console.log('========== 加载老人列表 ==========')
    console.log('当前用户信息:', userInfo)
    console.log('用户角色:', userInfo.role)
    console.log('用户姓名:', userInfo.realName)
    console.log('用户护理区:', userInfo.careZone)
    console.log('Token:', token)
    console.log('====================================')
    
    app.request('/resident/list?current=1&size=100', 'GET').then(res => {
      wx.hideLoading()
      
      console.log('========== 老人列表响应 ==========')
      console.log('响应数据:', res)
      
      let residentList = []
      
      // 处理分页响应
      if (res && res.records && Array.isArray(res.records)) {
        residentList = res.records.map(app.normalizeUser)
        console.log('分页数据，总数:', res.total)
      } else if (res && Array.isArray(res)) {
        residentList = res.map(app.normalizeUser)
      }
      
      console.log('老人列表:', residentList)
      console.log('老人数量:', residentList.length)
      
      // 过滤出在院老人
      residentList = residentList.filter(p => p.status === 'RESIDENT')
      
      console.log('在院老人数量:', residentList.length)
      console.log('====================================')
      
      if (residentList.length === 0) {
        wx.showModal({
          title: '提示',
          content: '当前没有可用的在住老人。\n\n照护主管只能管理自己负责的老人，护理员只能管理本护理区老人。\n\n您的护理区：' + (userInfo.careZone || '未设置'),
          showCancel: false
        })
      }
      
      this.setData({ residentList })
      
      console.log('加载老人列表成功:', residentList.length, '个老人')
    }).catch(err => {
      wx.hideLoading()
      console.error('加载老人列表失败:', err)
      
      const errorMsg = err.msg || err.message || '加载失败'
      wx.showModal({
        title: '加载老人列表失败',
        content: errorMsg + '\n\n请检查网络连接或联系管理员。\n\n您的护理区：' + (userInfo.careZone || '未设置'),
        showCancel: false
      })
      
      this.setData({ residentList: [] })
    })
  },

  onResidentPickerChange(e) {
    const index = e.detail.value
    const resident = this.data.residentList[index]
    this.setData({ 
      selectedResident: resident,
      selectedResidentIndex: index
    })
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    this.setData({
      [`formData.${field}`]: value
    })
  },

  onUnitChange(e) {
    const index = e.detail.value
    const unit = this.data.unitOptions[index]
    this.setData({
      unitIndex: index,
      'formData.unit': unit
    })
  },

  onFrequencyChange(e) {
    const index = e.detail.value
    const frequency = this.data.frequencyOptions[index]
    this.setData({
      frequencyIndex: index,
      'formData.frequency': frequency
    })
  },

  onRouteChange(e) {
    const index = e.detail.value
    const route = this.data.routeOptions[index]
    this.setData({
      routeIndex: index,
      'formData.route': route
    })
  },

  onStartDateChange(e) {
    this.setData({
      'formData.startDate': e.detail.value
    })
  },

  onEndDateChange(e) {
    this.setData({
      'formData.endDate': e.detail.value
    })
  },

  onDurationInput(e) {
    const value = parseInt(e.detail.value) || null
    this.setData({
      'formData.duration': value
    })
  },

  validateForm() {
    if (!this.data.selectedResident) {
      wx.showToast({ title: '请选择老人', icon: 'none' })
      return false
    }

    const { medicationName, dosage, frequency, route, startDate } = this.data.formData
    
    if (!medicationName || !medicationName.trim()) {
      wx.showToast({ title: '请输入药品名称', icon: 'none' })
      return false
    }
    
    if (!dosage || !dosage.trim()) {
      wx.showToast({ title: '请输入剂量', icon: 'none' })
      return false
    }
    
    if (!frequency) {
      wx.showToast({ title: '请选择用药频次', icon: 'none' })
      return false
    }
    
    if (!route) {
      wx.showToast({ title: '请选择给药途径', icon: 'none' })
      return false
    }
    
    if (!startDate) {
      wx.showToast({ title: '请选择开始日期', icon: 'none' })
      return false
    }
    
    return true
  },

  onSubmit() {
    if (!this.validateForm()) {
      return
    }

    const resident = this.data.selectedResident

    wx.showLoading({ title: '保存中...', mask: true })

    // 构建老人用药记录提交数据
    const medicationData = {
      residentId: resident.id,
      medicationName: this.data.formData.medicationName,
      dosage: this.data.formData.dosage,
      unit: this.data.formData.unit,
      frequency: this.data.formData.frequency,
      route: this.data.formData.route,
      startDate: this.data.formData.startDate,
      endDate: this.data.formData.endDate || null,
      duration: this.data.formData.duration,
      purpose: this.data.formData.purpose,
      notes: this.data.formData.notes
    }

    // 调用用药管理API
    app.request('/medication', 'POST', medicationData).then(res => {
      wx.hideLoading()
      wx.showToast({
        title: '添加成功',
        icon: 'success',
        duration: 2000
      })

      // 重置表单
      setTimeout(() => {
        this.resetForm()
      }, 2000)
    }).catch(err => {
      wx.hideLoading()
      console.error('添加用药记录失败:', err)
      
      const errorMsg = err.msg || err.message || '添加失败'
      wx.showModal({
        title: '添加失败',
        content: errorMsg,
        showCancel: false
      })
    })
  },

  resetForm() {
    this.setData({
      selectedResident: null,
      selectedResidentIndex: -1,
      formData: {
        medicationName: '',
        dosage: '',
        unit: 'mg',
        frequency: '',
        route: '',
        startDate: '',
        endDate: '',
        duration: '',
        purpose: '',
        notes: ''
      },
      unitIndex: 0,
      frequencyIndex: 0,
      routeIndex: 0
    })
    this.initDefaultDate()
  },

  onCancel() {
    wx.navigateBack()
  }
})

