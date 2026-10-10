// 老人查看评估报告页面
const app = getApp()

Page({
  data: {
    reports: [],
    loading: false,
    userId: null
  },

  onLoad(options) {
    console.log('页面加载，options:', options)
    console.log('全局userInfo:', app.globalData.userInfo)
    
    // 使用数字 userId（数据库主键），而非 residentId 字符串
    const userId = options.userId || app.globalData.userInfo?.id
    
    console.log('获取到的userId:', userId)
    
    if (userId) {
      this.setData({ userId })
      this.loadReports()
    } else {
      wx.showToast({
        title: '无法获取用户信息',
        icon: 'none'
      })
    }
  },

  loadReports() {
    this.setData({ loading: true })

    console.log('开始加载评估报告，userId:', this.data.userId)
    console.log('API地址:', `${app.globalData.baseUrl}/risk/resident-reports/${this.data.userId}`)

    wx.request({
      url: `${app.globalData.baseUrl}/risk/resident-reports/${this.data.userId}`,
      method: 'GET',
      header: {
        'content-type': 'application/json',
        'Authorization': wx.getStorageSync('token') || ''
      },
      success: (res) => {
        console.log('API响应:', res)
        
        if (res.data.code === 200) {
          const reports = (res.data.data || []).map(item => ({
            ...item,
            evaluateTime: this.formatDateTime(item.evaluateTime),
            riskLevel: this.getRiskLevelCode(item.riskLevel)
          }))
          
          this.setData({ reports })
          
          if (reports.length === 0) {
            wx.showToast({ title: '暂无评估报告', icon: 'none' })
          }
        } else {
          console.error('API返回错误:', res.data)
          wx.showToast({
            title: res.data.message || res.data.msg || '加载失败',
            icon: 'none'
          })
        }
      },
      fail: (err) => {
        console.error('网络请求失败:', err)
        wx.showToast({ title: '网络错误，请检查后端服务', icon: 'none' })
      },
      complete: () => {
        this.setData({ loading: false })
      }
    })
  },

  viewDetail(e) {
    const report = e.currentTarget.dataset.report
    wx.navigateTo({
      url: `/pages/risk/assessment-detail?id=${report.id}`
    })
  },

  onPullDownRefresh() {
    this.loadReports()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  },

  formatDateTime(dateStr) {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}`
  },

  getRiskLevelCode(level) {
    const codeMap = {
      'HIGH': 'high',
      'MEDIUM': 'medium',
      'LOW': 'low'
    }
    return codeMap[level] || 'low'
  }
})
