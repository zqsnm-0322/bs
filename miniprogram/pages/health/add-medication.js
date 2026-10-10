const app = getApp()

Page({
  data: {
    medicationId: null,
    residentId: null,
    isEdit: false,
    formData: {
      name: '',
      dosage: '',
      frequency: '',
      times: ['08:00'],
      duration: '',
      startDate: '',
      endDate: '',
      instructions: '',
      sideEffects: ''
    },
    frequencyOptions: ['每日一次', '每日两次', '每日三次', '每日四次', '每周一次', '按需服用'],
    frequencyIndex: 0
  },

  onLoad(options) {
    // 权限检查：老人不能添加或编辑用药记录
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    if (userInfo.role === 'RESIDENT') {
      wx.showModal({
        title: '权限不足',
        content: '老人只能查看用药记录，不能进行编辑操作',
        showCancel: false,
        success: () => {
          wx.navigateBack()
        }
      })
      return
    }

    // 设置默认开始日期为今天
    const today = new Date()
    const dateStr = this.formatDate(today)
    this.setData({
      'formData.startDate': dateStr
    })

    // 如果有 id 参数，说明是编辑模式
    if (options.id) {
      this.setData({
        medicationId: options.id,
        residentId: options.residentId,
        isEdit: true
      })
      this.loadMedicationDetail(options.id)
    } else if (options.residentId) {
      this.setData({
        residentId: options.residentId
      })
    }
  },

  // 停用药品
  onStopMedication() {
    wx.showModal({
      title: '确认停用',
      content: '确定要停用该药品吗？停用后将无法恢复。',
      editable: true,
      placeholderText: '请输入停用原因(可选)',
      success: (res) => {
        if (res.confirm) {
          const stopReason = res.content || ''
          this.stopMedication(stopReason)
        }
      }
    })
  },

  stopMedication(stopReason) {
    wx.showLoading({ title: '停用中...', mask: true })

    const requestData = stopReason ? { stopReason } : {}

    app.request(`/medication/${this.data.medicationId}/stop`, 'PUT', requestData).then(() => {
      wx.hideLoading()
      wx.showToast({
        title: '停用成功',
        icon: 'success',
        duration: 2000
      })

      setTimeout(() => {
        wx.navigateBack({
          success: () => {
            const pages = getCurrentPages()
            const prevPage = pages[pages.length - 2]
            if (prevPage && prevPage.loadMedicationData) {
              prevPage.loadMedicationData()
            } else if (prevPage && prevPage.loadMedications) {
              prevPage.loadMedications()
            }
          }
        })
      }, 2000)
    }).catch(err => {
      wx.hideLoading()
      console.error('停用药品失败:', err)
      wx.showToast({
        title: '停用失败',
        icon: 'none'
      })
    })
  },

  // 加载用药详情
  loadMedicationDetail(id) {
    wx.showLoading({ title: '加载中...', mask: true })
    
    app.request(`/medication/${id}`, 'GET').then(res => {
      wx.hideLoading()
      
      // 处理 times 字段
      let times = ['08:00']
      if (res.times) {
        if (typeof res.times === 'string') {
          times = res.times.split(',').map(t => t.trim())
        } else if (Array.isArray(res.times)) {
          times = res.times
        }
      }

      // 查找频次索引
      const frequencyIndex = this.data.frequencyOptions.indexOf(res.frequency)

      this.setData({
        'formData.name': res.medicationName || '',
        'formData.dosage': res.dosage || '',
        'formData.frequency': res.frequency || '',
        'formData.times': times,
        'formData.duration': res.duration || '',
        'formData.startDate': res.startDate ? this.formatDate(new Date(res.startDate)) : '',
        'formData.endDate': res.endDate ? this.formatDate(new Date(res.endDate)) : '',
        'formData.instructions': res.instructions || '',
        'formData.sideEffects': res.sideEffects || '',
        frequencyIndex: frequencyIndex >= 0 ? frequencyIndex : 0
      })
    }).catch(err => {
      wx.hideLoading()
      console.error('加载用药信息失败:', err)
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    })
  },

  formatDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    this.setData({
      [`formData.${field}`]: value
    })
  },

  onFrequencyChange(e) {
    const index = e.detail.value
    const frequency = this.data.frequencyOptions[index]
    
    // 根据频次自动设置服药时间
    let times = []
    switch(frequency) {
      case '每日一次':
        times = ['08:00']
        break
      case '每日两次':
        times = ['08:00', '20:00']
        break
      case '每日三次':
        times = ['08:00', '14:00', '20:00']
        break
      case '每日四次':
        times = ['08:00', '12:00', '16:00', '20:00']
        break
      default:
        times = ['08:00']
    }
    
    this.setData({
      frequencyIndex: index,
      'formData.frequency': frequency,
      'formData.times': times
    })
  },

  onTimeChange(e) {
    const index = e.currentTarget.dataset.index
    const time = e.detail.value
    const times = [...this.data.formData.times]
    times[index] = time
    this.setData({
      'formData.times': times
    })
  },

  addTime() {
    const times = [...this.data.formData.times]
    if (times.length >= 6) {
      wx.showToast({
        title: '最多添加6个时间',
        icon: 'none'
      })
      return
    }
    times.push('08:00')
    this.setData({
      'formData.times': times
    })
  },

  removeTime(e) {
    const index = e.currentTarget.dataset.index
    const times = [...this.data.formData.times]
    if (times.length <= 1) {
      wx.showToast({
        title: '至少保留一个时间',
        icon: 'none'
      })
      return
    }
    times.splice(index, 1)
    this.setData({
      'formData.times': times
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

  validateForm() {
    const { name, dosage, frequency, times, startDate } = this.data.formData
    
    if (!name || !name.trim()) {
      wx.showToast({ title: '请输入药物名称', icon: 'none' })
      return false
    }
    
    if (!dosage || !dosage.trim()) {
      wx.showToast({ title: '请输入剂量', icon: 'none' })
      return false
    }
    
    if (!frequency) {
      wx.showToast({ title: '请选择服药频次', icon: 'none' })
      return false
    }
    
    if (!times || times.length === 0) {
      wx.showToast({ title: '请设置服药时间', icon: 'none' })
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

    const userInfo = app.globalData.userInfo || {}
    const residentId = this.data.residentId || userInfo.residentId || userInfo.id

    if (!residentId) {
      wx.showToast({ title: '老人信息缺失', icon: 'none' })
      return
    }

    wx.showLoading({ title: '保存中...', mask: true })

    // 构建提交数据
    const medicationData = {
      residentId: residentId,
      residentName: userInfo.name || '老人',
      medicationName: this.data.formData.name,
      dosage: this.data.formData.dosage,
      frequency: this.data.formData.frequency,
      times: this.data.formData.times,
      duration: this.data.formData.duration,
      startDate: this.data.formData.startDate,
      endDate: this.data.formData.endDate,
      instructions: this.data.formData.instructions,
      sideEffects: this.data.formData.sideEffects
    }

    // 根据是否是编辑模式选择不同的 API
    const url = this.data.isEdit ? `/medication/${this.data.medicationId}` : '/medication'
    const method = this.data.isEdit ? 'PUT' : 'POST'

    // 调用后端API保存数据
    app.request(url, method, medicationData).then(res => {
      wx.hideLoading()
      wx.showToast({
        title: this.data.isEdit ? '修改成功' : '添加成功',
        icon: 'success',
        duration: 2000
      })

      // 返回上一页并刷新
      setTimeout(() => {
        wx.navigateBack({
          success: () => {
            // 通知上一页刷新数据
            const pages = getCurrentPages()
            const prevPage = pages[pages.length - 2]
            if (prevPage && prevPage.loadMedicationData) {
              prevPage.loadMedicationData()
            } else if (prevPage && prevPage.loadMedications) {
              prevPage.loadMedications()
            }
          }
        })
      }, 2000)
    }).catch(err => {
      wx.hideLoading()
      console.error('保存用药记录失败:', err)
      wx.showToast({
        title: this.data.isEdit ? '修改失败' : '添加失败',
        icon: 'none'
      })
    })
  },

  onCancel() {
    wx.navigateBack()
  }
})
