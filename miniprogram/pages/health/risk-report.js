const app = getApp()

Page({
  data: {
    loading: false,
    hasReport: false,
    report: {
      assessmentTime: '',
      riskLevel: '',
      riskLevelText: '',
      riskLevelColor: '',
      healthStatus: '',
      healthAdvice: []
    }
  },

  onLoad() {
    this.loadRiskReport()
  },

  onShow() {
    // 每次显示页面时刷新数据
    this.loadRiskReport()
  },

  // 加载风险评估报告
  loadRiskReport() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo')
    const residentId = userInfo?.residentId || userInfo?.id
    
    if (!residentId) {
      wx.showToast({
        title: '获取老人信息失败',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })

    app.request(`/enhanced-risk/resident-report/${residentId}`, 'GET')
      .then(res => {
        console.log('风险评估报告:', res)
        
        this.setData({
          hasReport: true,
          report: {
            assessmentTime: res.assessmentTime,
            riskLevel: res.riskLevel,
            riskLevelText: res.riskLevelText,
            riskLevelColor: res.riskLevelColor,
            healthStatus: res.healthStatus,
            healthAdvice: res.healthAdvice || []
          },
          loading: false
        })
      })
      .catch(err => {
        console.error('加载风险评估报告失败:', err)
        
        this.setData({
          hasReport: false,
          loading: false
        })
        
        // 如果是没有记录的错误，不显示错误提示
        if (err.message && err.message.includes('暂无风险评估记录')) {
          // 静默处理
        } else {
          wx.showToast({
            title: '加载失败',
            icon: 'none'
          })
        }
      })
  },

  // 格式化日期
  formatDate(dateStr) {
    if (!dateStr) return ''
    
    try {
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      
      return `${year}-${month}-${day} ${hour}:${minute}`
    } catch (e) {
      return dateStr
    }
  },

  // 联系照护主管
  contactCareManager() {
    wx.showModal({
      title: '联系照护主管',
      content: '如有紧急情况，请立即联系照护主管或养老院值班人员；需要医疗救治时请拨打急救电话。',
      showCancel: false
    })
  },

  // 查看历史记录
  viewHistory() {
    wx.showToast({
      title: '历史记录功能开发中',
      icon: 'none'
    })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadRiskReport()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
