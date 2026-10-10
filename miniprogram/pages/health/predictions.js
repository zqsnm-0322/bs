const app = getApp()

Page({
  data: {
    residentId: '',
    residentInfo: {},
    predictions: [],
    assessmentTime: '',
    riskLevel: '',
    riskLevelText: '',
    totalScore: 0,
    loading: true
  },

  onLoad(options) {
    const residentId = options.residentId
    if (residentId) {
      this.setData({ residentId })
      this.loadResidentInfo(residentId)
      this.loadPredictions(residentId)
    } else {
      wx.showToast({
        title: '参数错误',
        icon: 'error'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }
  },

  // 加载老人信息
  loadResidentInfo(residentId) {
    // 这里可以调用获取老人信息的接口
    // 暂时使用全局用户信息
    const userInfo = app.globalData.userInfo || {}
    this.setData({
      residentInfo: {
        name: userInfo.name || '老人',
        residentNo: userInfo.residentNo || userInfo.residentId || '-'
      }
    })
  },

  // 加载照护风险提示
  loadPredictions(residentId) {
    wx.showLoading({ title: '加载中...' })
    
    app.request(`/enhanced-risk/predictions/${residentId}`).then(res => {
      if (res && res.predictions) {
        this.setData({
          predictions: res.predictions,
          assessmentTime: this.formatDateTime(res.assessmentTime),
          riskLevel: res.riskLevel,
          riskLevelText: this.getRiskLevelText(res.riskLevel),
          totalScore: res.totalScore,
          loading: false
        })
      } else {
        this.setData({
          predictions: [],
          loading: false
        })
      }
    }).catch(err => {
      console.error('加载照护风险提示失败:', err)
      wx.showToast({
        title: '加载失败',
        icon: 'error'
      })
      this.setData({
        predictions: [],
        loading: false
      })
    }).finally(() => {
      wx.hideLoading()
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

  // 获取风险等级文本
  getRiskLevelText(level) {
    const texts = {
      'LOW': '低风险',
      'MEDIUM': '中风险', 
      'HIGH': '高风险',
      'CRITICAL': '极高风险'
    }
    return texts[level] || level
  },

  // 分享给家属
  shareWithFamily() {
    const shareData = {
      title: '养老照护风险提示',
      desc: `${this.data.residentInfo.name}的养老照护风险报告，当前风险等级：${this.data.riskLevelText}`,
      path: `/pages/health/predictions?residentId=${this.data.residentId}`
    }
    
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    })
    
    wx.showToast({
      title: '请点击右上角分享',
      icon: 'none'
    })
  },

  // 分享配置
  onShareAppMessage() {
    return {
      title: `${this.data.residentInfo.name}的养老照护风险提示`,
      desc: `当前风险等级：${this.data.riskLevelText}，共${this.data.predictions.length}项照护风险提示`,
      path: `/pages/health/predictions?residentId=${this.data.residentId}`
    }
  },

  onShareTimeline() {
    return {
      title: `养老照护风险提示 - ${this.data.riskLevelText}`,
      query: `residentId=${this.data.residentId}`
    }
  }
})
