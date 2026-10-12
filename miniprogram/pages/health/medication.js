const app = getApp()

Page({
  data: {
    userInfo: {},
    medications: [],
    reminders: [],
    activeTab: 'medications',
    selectedMedication: null,
    showDetail: false
  },

  onLoad() {
    this.loadUserInfo()
    this.loadMedicationData()
    this.checkReminders()
  },

  onShow() {
    this.checkReminders()
  },

  loadUserInfo() {
    const userInfo = app.globalData.userInfo || {}
    this.setData({ userInfo })
  },

  // 需求8: 用药记录添加结束日期
  loadMedicationData() {
    const userInfo = app.globalData.userInfo || {}
    const residentId = userInfo.id || userInfo.residentId
    
    if (!residentId) {
      console.log('没有老人ID，无法加载用药数据')
      return
    }

    wx.showLoading({ title: '加载中...' })
    
    // 从后端获取用药数据
    app.request(`/medication/resident/${residentId}`, 'GET').then(res => {
      wx.hideLoading()
      
      if (res && Array.isArray(res)) {
        // 需求8: 格式化日期,包括结束日期
        const medications = res.map(item => ({
          ...item,
          startDate: this.formatDate(item.startDate),
          endDate: item.endDate ? this.formatDate(item.endDate) : null
        }))
        this.setData({ medications })
      } else {
        this.setData({ medications: [] })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error('加载用药数据失败:', err)
      this.setData({ medications: [] })
    })
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  checkReminders() {
    // 检查是否有待服用的药物提醒
    const pendingReminders = this.data.reminders.filter(item => item.status === 'pending')
    if (pendingReminders.length > 0) {
      // 可以显示提醒通知
    }
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
  },

  viewMedicationDetail(e) {
    const id = e.currentTarget.dataset.id
    const medication = this.data.medications.find(item => item.id === id)
    
    if (medication) {
      this.setData({
        selectedMedication: medication,
        showDetail: true
      })
    }
  },

  closeMedicationDetail() {
    this.setData({
      selectedMedication: null,
      showDetail: false
    })
  },

  takeMedication(e) {
    const statusMap = {
      'ACTIVE': '进行中',
      'PAUSED': '已暂停',
      'COMPLETED': '已完成',
      'STOPPED': '已停止'
    }
    return statusMap[status] || status
  },

  getMedicationStatusColor(status) {
    const colorMap = {
      'ACTIVE': '#52c41a',
      'PAUSED': '#faad14',
      'COMPLETED': '#1890ff',
      'STOPPED': '#f5222d'
    }
    return colorMap[status] || '#666'
  },

  getReminderStatusText(status) {
    const statusMap = {
      'pending': '待服用',
      'taken': '已服用',
      'skipped': '已跳过',
      'missed': '已错过'
    }
    return statusMap[status] || '未知'
  },

  getReminderStatusColor(status) {
    const colorMap = {
      'pending': '#faad14',
      'taken': '#52c41a',
      'skipped': '#999',
      'missed': '#f5222d'
    }
    return colorMap[status] || '#666'
  },

  onPullDownRefresh() {
    this.loadMedicationData()
    this.checkReminders()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
