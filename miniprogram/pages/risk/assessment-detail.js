const app = getApp()

Page({
  data: {
    assessmentId: '',
    detail: {},
    loading: true
  },

  onLoad(options) {
    const cachedDetail = wx.getStorageSync('selectedAssessmentReport') || {}
    if (cachedDetail && String(cachedDetail.id || '') === String(options.id || '')) {
      this.setData({
        detail: this.normalizeDetail(cachedDetail),
        loading: false
      })
    }

    if (options.id) {
      this.setData({ assessmentId: options.id })
      this.loadDetail()
    }
  },

  loadDetail() {
    this.setData({ loading: true })
    
    app.request(`/enhanced-risk/report-detail/${this.data.assessmentId}`, 'GET').then(res => {
      this.setData({ 
        detail: this.normalizeDetail(res),
        loading: false
      })
    }).catch(err => {
      console.error('加载评估详情失败:', err)
      const hasCachedDetail = !!this.data.detail.id
      wx.showToast({
        title: hasCachedDetail ? '已显示报告摘要' : (err.message || '加载失败'),
        icon: 'none'
      })
      this.setData({ loading: false })
    })
  },

  normalizeDetail(detail = {}) {
    const riskLevelMap = {
      critical: 'CRITICAL',
      high: 'HIGH',
      medium: 'MEDIUM',
      low: 'LOW'
    }
    return {
      ...detail,
      residentName: detail.residentName || '-',
      bedNo: detail.bedNo || '-',
      careZone: detail.careZone || '-',
      scaleName: detail.scaleName && detail.scaleName !== '未知量表'
        ? detail.scaleName
        : '综合风险评估',
      evaluateTime: this.formatTime(detail.evaluateTime || detail.assessmentTime),
      evaluatorName: detail.evaluatorName || detail.assessedBy || detail.sentBy || '系统评估',
      riskLevel: riskLevelMap[detail.riskLevel] || detail.riskLevel,
      riskLevelText: detail.riskLevelText || this.getRiskLevelText(detail.riskLevel)
    }
  },

  getRiskLevelText(level) {
    const textMap = {
      CRITICAL: '极高风险',
      HIGH: '高风险',
      MEDIUM: '中风险',
      LOW: '低风险',
      high: '高风险',
      medium: '中风险',
      low: '低风险'
    }
    return textMap[level] || '-'
  },

  // 格式化时间
  formatTime(dateTime) {
    if (!dateTime) return ''
    const date = new Date(dateTime)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  },

    // 返回老人详情
  goBack() {
    wx.navigateBack()
  }
})
