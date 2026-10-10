const app = getApp()

Page({
  data: {
    assessmentId: '',
    detail: {},
    loading: true
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ assessmentId: options.id })
      this.loadDetail()
    }
  },

  loadDetail() {
    this.setData({ loading: true })
    
    app.request(`/risk/detail/${this.data.assessmentId}`, 'GET').then(res => {
      this.setData({ 
        detail: res,
        loading: false
      })
    }).catch(err => {
      console.error('加载评估详情失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    })
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
