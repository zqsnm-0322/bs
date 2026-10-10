const app = getApp()

Page({
  data: {
    residentId: '',
    residentInfo: {},
    riskHistory: [],
    medications: [],
    userInfo: {},
    loading: true,
    historyLoading: false,
    medicationLoading: false,
    predictionsLoading: false,
    healthPredictions: [],
    assessmentTime: ''
  },

  onLoad(options) {
    // 获取用户信息
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    this.setData({ userInfo })
    
    // 支持 id 和 residentId 两种参数
    const residentId = options.id || options.residentId
    if (residentId) {
      this.setData({ residentId: residentId })
      this.loadResidentDetail()
      this.loadRiskHistory()
      this.loadMedications()
      this.loadHealthPredictions()
    } else {
      wx.showToast({ 
        title: '老人ID缺失', 
        icon: 'none',
        complete: () => {
          setTimeout(() => {
            wx.navigateBack()
          }, 1500)
        }
      })
    }
  },

  // 加载老人档案
  loadResidentDetail() {
    this.setData({ loading: true })
    
    app.request(`/resident/${this.data.residentId}`, 'GET').then(res => {
      this.setData({ 
        residentInfo: app.normalizeUser(res),
        loading: false
      })
    }).catch(err => {
      console.error('加载老人档案失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    })
  },

  // 加载风险评估历史
  loadRiskHistory() {
    this.setData({ historyLoading: true })
    
    app.request(`/risk/history/${this.data.residentId}`, 'GET').then(res => {
      this.setData({ 
        riskHistory: res || [],
        historyLoading: false
      })
    }).catch(err => {
      console.error('加载评估历史失败:', err)
      this.setData({ historyLoading: false })
    })
  },

  // 加载用药信息
  loadMedications() {
    this.setData({ medicationLoading: true })
    
    app.request(`/medication/resident/${this.data.residentId}`, 'GET').then(res => {
      // 格式化日期和时间
      const medications = (res || []).map(item => {
        let timesStr = '-'
        if (item.times) {
          if (typeof item.times === 'string') {
            timesStr = item.times.replace(/,/g, '、')
          } else if (Array.isArray(item.times)) {
            timesStr = item.times.join('、')
          } else {
            timesStr = String(item.times)
          }
        }
        
        return {
          ...item,
          startDate: this.formatDate(item.startDate),
          endDate: item.endDate ? this.formatDate(item.endDate) : null,
          times: timesStr
        }
      })
      
      this.setData({ 
        medications: medications,
        medicationLoading: false
      })
    }).catch(err => {
      console.error('加载用药信息失败:', err)
      this.setData({ medicationLoading: false })
    })
  },

  // 加载照护风险提示
  loadHealthPredictions() {
    this.setData({ predictionsLoading: true })
    
    app.request(`/enhanced-risk/predictions/${this.data.residentId}`).then(res => {
      if (res && res.predictions) {
        this.setData({
          healthPredictions: res.predictions || [],
          assessmentTime: this.formatDateTime(res.assessmentTime),
          predictionsLoading: false
        })
      } else {
        this.setData({
          healthPredictions: [],
          assessmentTime: '',
          predictionsLoading: false
        })
      }
    }).catch(err => {
      console.error('加载照护风险提示失败:', err)
      this.setData({
        healthPredictions: [],
        assessmentTime: '',
        predictionsLoading: false
      })
    })
  },

  // 查看预测详情
  viewPredictionDetail(e) {
    const prediction = e.currentTarget.dataset.prediction
    if (prediction) {
      let content = `风险评分：${prediction.probability || prediction.score || '-'}\n`
      content += `关注周期：${prediction.timeframe || '持续观察'}\n\n`
      
      if (prediction.symptoms && prediction.symptoms.length > 0) {
        content += `可能症状：\n${prediction.symptoms.map(s => `• ${s}`).join('\n')}\n\n`
      }
      
      if (prediction.interventions && prediction.interventions.length > 0) {
        content += `建议措施：\n${prediction.interventions.map(i => `• ${i}`).join('\n')}`
      }
      
      wx.showModal({
        title: prediction.condition,
        content: content,
        showCancel: false,
        confirmText: '知道了'
      })
    }
  },

  // 查看所有预测
  viewAllPredictions() {
    wx.navigateTo({
      url: `/pages/health/predictions?residentId=${this.data.residentId}`
    })
  },

  // 格式化日期时间
  formatDateTime(dateTime) {
    if (!dateTime) return '-'
    const date = new Date(dateTime)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  },

  // 格式化日期
  formatDate(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  // 快速评估
  goAssess() {
    wx.navigateTo({
      url: `/pages/risk/assess?residentId=${this.data.residentId}&residentName=${this.data.residentInfo.name}`
    })
  },

  // 联系家属
  callFamily() {
    const phone = this.data.residentInfo.emergencyContactPhone || this.data.residentInfo.emergencyPhone
    if (phone) {
      wx.makePhoneCall({
        phoneNumber: phone,
        fail: () => {
          wx.showToast({ title: '拨打失败', icon: 'none' })
        }
      })
    } else {
      wx.showToast({ title: '暂无联系方式', icon: 'none' })
    }
  },

  // 查看评估详情
  viewAssessmentDetail(e) {
    const { id } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/risk/assessment-detail?id=${id}`
    })
  },

  // 添加用药信息（照护主管专用）
  addMedication() {
    // 检查权限
    if (this.data.userInfo.role !== 'CARE_MANAGER') {
      wx.showToast({
        title: '只有照护主管可以添加用药',
        icon: 'none'
      })
      return
    }

    // 跳转到用药管理页面，并传递老人ID
    wx.navigateTo({
      url: `/pages/care-manager/medication/index?residentId=${this.data.residentId}&residentName=${this.data.residentInfo.name}`
    })
  },

  // 编辑用药信息
  editMedication(e) {
    const { id } = e.currentTarget.dataset
    
    // 检查权限
    if (this.data.userInfo.role !== 'CARE_MANAGER') {
      wx.showToast({
        title: '只有照护主管可以编辑用药',
        icon: 'none'
      })
      return
    }
    
    wx.navigateTo({
      url: `/pages/care-manager/medication/index?id=${id}&residentId=${this.data.residentId}`
    })
  },

  // 停用药品
  stopMedication(e) {
    const { id, name } = e.currentTarget.dataset
    
    wx.showModal({
      title: '确认停用',
      content: `确定要停用药品"${name}"吗？停用后将无法恢复。`,
      editable: true,
      placeholderText: '请输入停用原因(可选)',
      success: (res) => {
        if (res.confirm) {
          const stopReason = res.content || ''
          this.performstopMedication(id, stopReason)
        }
      }
    })
  },

  // 执行停用操作
  performstopMedication(medicationId, stopReason) {
    wx.showLoading({ title: '停用中...', mask: true })

    const requestData = stopReason ? { stopReason } : {}

    app.request(`/medication/${medicationId}/stop`, 'PUT', requestData).then(() => {
      wx.hideLoading()
      wx.showToast({
        title: '停用成功',
        icon: 'success'
      })
      
      // 重新加载用药信息
      this.loadMedications()
    }).catch(err => {
      wx.hideLoading()
      console.error('停用药品失败:', err)
      wx.showToast({
        title: '停用失败',
        icon: 'none'
      })
    })
  },

  // 恢复药品
  resumeMedication(e) {
    const { id, name } = e.currentTarget.dataset
    
    wx.showModal({
      title: '确认恢复',
      content: `确定要恢复药品"${name}"的用药吗？`,
      success: (res) => {
        if (res.confirm) {
          this.performresumeMedication(id)
        }
      }
    })
  },

  // 执行恢复操作
  performresumeMedication(medicationId) {
    wx.showLoading({ title: '恢复中...', mask: true })

    app.request(`/medication/${medicationId}/resume`, 'PUT').then(() => {
      wx.hideLoading()
      wx.showToast({
        title: '恢复成功',
        icon: 'success'
      })
      
      // 重新加载用药信息
      this.loadMedications()
    }).catch(err => {
      wx.hideLoading()
      console.error('恢复药品失败:', err)
      wx.showToast({
        title: '恢复失败',
        icon: 'none'
      })
    })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadResidentDetail()
    this.loadRiskHistory()
    this.loadMedications()
    this.loadHealthPredictions()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  },

  // 分享老人信息
  onShareAppMessage() {
    return {
      title: `老人健康档案`,
      path: `/pages/resident/detail/detail?id=${this.data.residentId}`
    }
  }
})
