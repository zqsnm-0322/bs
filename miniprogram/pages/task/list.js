const app = getApp()

Page({
  data: {
    taskList: [],
    loading: false,
    activeTab: 'pending', // pending: 待办, completed: 已完成
    stats: {
      pending: 0,
      completed: 0,
      highRisk: 0
    }
  },

  onLoad() {
    this.loadTasks()
    this.loadStats()
  },

  onShow() {
    // 每次显示页面时刷新数据
    this.loadTasks()
    this.loadStats()
  },

  // 加载任务列表
  loadTasks() {
    this.setData({ loading: true })
    
    const status = this.data.activeTab === 'pending' ? 0 : 1
    
    app.request('/task/list', 'GET', { status }).then(res => {
      this.setData({ taskList: res || [] })
    }).catch(err => {
      console.error('加载任务失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  // 加载统计数据
  loadStats() {
    app.request('/task/stats', 'GET').then(res => {
      this.setData({ stats: res || {} })
    }).catch(err => {
      console.error('加载统计失败:', err)
    })
  },

  // 切换标签页
  onTabChange(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
    this.loadTasks()
  },

  // 查看任务详情
  viewTask(e) {
    const taskId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/task/detail?id=${taskId}`
    })
  },

  // 快速完成任务
  completeTask(e) {
    const taskId = e.currentTarget.dataset.id
    
    wx.showModal({
      title: '确认完成',
      content: '确认完成该任务吗？',
      success: (res) => {
        if (res.confirm) {
          app.request('/task/complete', 'POST', { taskId }).then(() => {
            wx.showToast({ title: '已完成', icon: 'success' })
            this.loadTasks()
            this.loadStats()
          }).catch(err => {
            wx.showToast({ title: '操作失败', icon: 'none' })
          })
        }
      }
    })
  },

  // 前往评估
  goAssess(e) {
    const { residentId, residentName } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/risk/assess?residentId=${residentId}&residentName=${residentName}`
    })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadTasks()
    this.loadStats()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
