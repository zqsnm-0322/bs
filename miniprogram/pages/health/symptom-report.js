const app = getApp()

Page({
  data: {
    userInfo: {},
    reportForm: {
      symptomType: '',
      severity: '',
      description: '',
      startTime: '',
      location: '',
      triggers: [],
      additionalInfo: ''
    },
    symptomTypes: [
      { id: 'pain', name: '疼痛', icon: '😣' },
      { id: 'nausea', name: '恶心呕吐', icon: '🤢' },
      { id: 'fever', name: '发热', icon: '🌡️' },
      { id: 'dizzy', name: '头晕', icon: '😵' },
      { id: 'breath', name: '呼吸困难', icon: '😮‍💨' },
      { id: 'sleep', name: '睡眠问题', icon: '😴' },
      { id: 'appetite', name: '食欲不振', icon: '🍽️' },
      { id: 'other', name: '其他', icon: '❓' }
    ],
    severityLevels: [
      { id: 'mild', name: '轻微', color: '#52c41a', desc: '轻微不适，不影响日常活动' },
      { id: 'moderate', name: '中等', color: '#faad14', desc: '明显不适，影响部分活动' },
      { id: 'severe', name: '严重', color: '#f5222d', desc: '严重不适，无法正常活动' }
    ],
    commonTriggers: [
      '活动后', '进食后', '休息时', '夜间', '天气变化', '情绪波动', '用药后', '其他'
    ],
    reportHistory: [],
    activeTab: 'report',
    submitting: false,
    // 需求5: 正在编辑的上报ID
    editingReportId: null
  },

  onLoad() {
    this.loadUserInfo()
    this.initForm()
    
    // 测试：打印初始triggers
    console.log('Initial triggers:', this.data.reportForm.triggers)
    console.log('Common triggers:', this.data.commonTriggers)
  },

  onShow() {
    if (this.data.activeTab === 'history') {
      this.loadReportHistory()
    }
  },

  loadUserInfo() {
    const userInfo = app.globalData.userInfo || {}
    this.setData({ userInfo })
  },

  initForm() {
    const now = new Date()
    const timeString = now.getFullYear() + '-' +
      String(now.getMonth() + 1).padStart(2, '0') + '-' +
      String(now.getDate()).padStart(2, '0') + ' ' +
      String(now.getHours()).padStart(2, '0') + ':' +
      String(now.getMinutes()).padStart(2, '0')

    this.setData({
      'reportForm.startTime': timeString
    })
  },

  // 加载上报历史
  loadReportHistory() {
    const residentId = this.data.userInfo.residentId || this.data.userInfo.id
    if (!residentId) return

    app.request(`/symptom/my?residentId=${residentId}`).then(res => {
      this.setData({ reportHistory: res.list || [] })
    }).catch(() => {
      this.setData({ reportHistory: [] })
    })
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
    if (tab === 'history') {
      this.loadReportHistory()
    }
  },

  selectSymptomType(e) {
    const type = e.currentTarget.dataset.type
    this.setData({ 'reportForm.symptomType': type })
  },

  selectSeverity(e) {
    const severity = e.currentTarget.dataset.severity
    this.setData({ 'reportForm.severity': severity })
  },

  onDescriptionInput(e) {
    this.setData({ 'reportForm.description': e.detail.value })
  },

  onLocationInput(e) {
    this.setData({ 'reportForm.location': e.detail.value })
  },

  onTimeChange(e) {
    const dateTime = e.detail.value
    const formattedTime = dateTime.replace('T', ' ').substring(0, 16)
    this.setData({ 'reportForm.startTime': formattedTime })
  },

  toggleTrigger(e) {
    const trigger = e.currentTarget.dataset.trigger
    if (!trigger) {
      console.error('trigger is undefined')
      return
    }
    
    const currentTriggers = this.data.reportForm.triggers || []
    const newTriggers = [...currentTriggers]
    const index = newTriggers.indexOf(trigger)

    if (index > -1) {
      // 已选中，取消选择
      newTriggers.splice(index, 1)
    } else {
      // 未选中，添加选择
      newTriggers.push(trigger)
    }

    this.setData({ 
      'reportForm.triggers': newTriggers 
    }, () => {
      console.log('triggers updated:', this.data.reportForm.triggers)
    })
  },

  onAdditionalInfoInput(e) {
    this.setData({ 'reportForm.additionalInfo': e.detail.value })
  },

  validateForm() {
    const form = this.data.reportForm

    if (!form.symptomType) {
      wx.showToast({ title: '请选择症状类型', icon: 'none' })
      return false
    }
    if (!form.severity) {
      wx.showToast({ title: '请选择严重程度', icon: 'none' })
      return false
    }
    if (!form.description.trim()) {
      wx.showToast({ title: '请描述症状详情', icon: 'none' })
      return false
    }
    if (!form.startTime) {
      wx.showToast({ title: '请选择开始时间', icon: 'none' })
      return false
    }
    return true
  },

  submitReport() {
    if (!this.validateForm()) return

    this.setData({ submitting: true })

    const residentId = this.data.userInfo.residentId || this.data.userInfo.id
    const reportData = {
      residentId: residentId,
      symptomType: this.data.reportForm.symptomType,
      severity: this.data.reportForm.severity,
      description: this.data.reportForm.description,
      location: this.data.reportForm.location,
      startTime: this.data.reportForm.startTime,
      triggers: this.data.reportForm.triggers,
      additionalInfo: this.data.reportForm.additionalInfo
    }

    // 需求5: 判断是新增还是修改
    const isEdit = !!this.data.editingReportId
    const url = isEdit 
      ? `/symptom/${this.data.editingReportId}/update` 
      : '/symptom/report'
    const method = isEdit ? 'PUT' : 'POST'

    app.request(url, method, reportData).then(res => {
      wx.showModal({
        title: isEdit ? '修改成功' : '上报成功',
        content: isEdit ? '症状信息已更新' : '症状已成功上报，护理员将尽快处理',
        showCancel: false,
        success: () => {
          this.resetForm()
          this.setData({ 
            activeTab: 'history',
            editingReportId: null 
          })
          this.loadReportHistory()
        }
      })
    }).catch(err => {
      wx.showToast({ title: err.message || (isEdit ? '修改失败' : '上报失败'), icon: 'none' })
    }).finally(() => {
      this.setData({ submitting: false })
    })
  },

  resetForm() {
    const now = new Date()
    const timeString = now.getFullYear() + '-' +
      String(now.getMonth() + 1).padStart(2, '0') + '-' +
      String(now.getDate()).padStart(2, '0') + ' ' +
      String(now.getHours()).padStart(2, '0') + ':' +
      String(now.getMinutes()).padStart(2, '0')

    this.setData({
      reportForm: {
        symptomType: '',
        severity: '',
        description: '',
        startTime: timeString,
        location: '',
        triggers: [], // 确保是空数组
        additionalInfo: ''
      },
      editingReportId: null // 需求5: 清除编辑状态
    })
  },

  // 需求5: 修改症状上报 - 只有未处理的可以修改
  editReport(e) {
    const id = e.currentTarget.dataset.id
    const report = this.data.reportHistory.find(item => item.id === id)
    
    if (!report) return
    
    // 只有待处理状态才能修改
    if (report.status !== 'PENDING') {
      wx.showToast({ 
        title: '只有未处理的上报才能修改', 
        icon: 'none' 
      })
      return
    }
    
    // 回填表单数据
    this.setData({
      'reportForm.symptomType': report.symptomType,
      'reportForm.severity': report.severity,
      'reportForm.description': report.description,
      'reportForm.location': report.location || '',
      'reportForm.startTime': report.startTime,
      'reportForm.triggers': report.triggers || [],
      'reportForm.additionalInfo': report.additionalInfo || '',
      editingReportId: id,
      activeTab: 'report'
    })
    
    wx.showToast({ 
      title: '已加载上报信息，可以修改', 
      icon: 'success' 
    })
  },

  viewReportDetail(e) {
    const id = e.currentTarget.dataset.id
    const report = this.data.reportHistory.find(item => item.id === id)

    if (report) {
      let content = `症状：${report.symptomName}\n严重程度：${report.severityName}\n`
      if (report.location) {
        content += `部位：${report.location}\n`
      }
      content += `状态：${report.statusText}`
      if (report.handleResult) {
        content += `\n护理员回复：${report.handleResult}`
      }

      // 需求5: 如果是待处理状态，显示修改按钮
      const showEdit = report.status === 'PENDING'

      wx.showModal({
        title: '症状详情',
        content: content,
        showCancel: showEdit,
        cancelText: '修改',
        confirmText: '关闭',
        success: (res) => {
          if (res.cancel && showEdit) {
            this.editReport(e)
          }
        }
      })
    }
  },

  onPullDownRefresh() {
    if (this.data.activeTab === 'history') {
      this.loadReportHistory()
    }
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
