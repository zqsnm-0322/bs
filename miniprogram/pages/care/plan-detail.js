// 护理计划详情页面
Page({
  data: {
    planId: null,
    plan: null,
    tasks: [],
    loading: true
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ planId: options.id })
      this.loadPlanDetail()
    }
  },

  loadPlanDetail() {
    wx.showLoading({ title: '加载中...', mask: true })
    
    wx.request({
      url: getApp().globalData.baseUrl + `/care-plan/${this.data.planId}`,
      method: 'GET',
      success: (res) => {
        wx.hideLoading()
        if (res.data.code === 200) {
          const data = res.data.data
          this.setData({
            plan: data.plan,
            tasks: data.tasks,
            loading: false
          })
        } else {
          wx.showToast({ title: res.data.message || '加载失败', icon: 'none' })
          setTimeout(() => wx.navigateBack(), 1500)
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '网络错误', icon: 'none' })
        setTimeout(() => wx.navigateBack(), 1500)
      }
    })
  },

  completeTask(e) {
    const taskId = e.currentTarget.dataset.id
    const userInfo = wx.getStorageSync('userInfo')
    
    if (!userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    wx.showModal({
      title: '确认完成',
      content: '确认完成此护理任务？',
      success: (res) => {
        if (res.confirm) {
          wx.request({
            url: getApp().globalData.baseUrl + `/care-plan/task/${taskId}/complete`,
            method: 'PUT',
            data: {
              userId: userInfo.id,
              userName: userInfo.realName || userInfo.username
            },
            success: (res) => {
              if (res.data.code === 200) {
                wx.showToast({ title: '任务已完成', icon: 'success' })
                this.loadPlanDetail()
              } else {
                wx.showToast({ title: res.data.message || '操作失败', icon: 'none' })
              }
            },
            fail: () => {
              wx.showToast({ title: '网络错误', icon: 'none' })
            }
          })
        }
      }
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

  getPlanTypeLabel(type) {
    const typeMap = {
      'FALL_PREVENTION': '跌倒防护',
      'PRESSURE_PREVENTION': '压疮预防',
      'REHABILITATION': '康复护理',
      'RESPIRATORY': '呼吸训练',
      'MEDICATION': '用药管理',
      'NUTRITION': '营养支持',
      'WOUND_CARE': '伤口护理',
      'PAIN_MANAGEMENT': '疼痛管理'
    }
    return typeMap[type] || type
  },

  getPriorityLabel(priority) {
    const priorityMap = {
      'HIGH': '高优先级',
      'MEDIUM': '中优先级',
      'LOW': '低优先级'
    }
    return priorityMap[priority] || priority
  },

  getStatusLabel(status) {
    const statusMap = {
      'ACTIVE': '进行中',
      'COMPLETED': '已完成',
      'CANCELLED': '已取消'
    }
    return statusMap[status] || status
  }
})
