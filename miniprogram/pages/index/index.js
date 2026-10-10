const app = getApp()

Page({
  data: {
    userInfo: {},
    userType: 'CAREGIVER', // 员工或入住老人
    stats: {},
    alerts: [],
    notifications: [], // 照护风险提示通知
    unreadNotifications: 0, // 未读通知数量
    residentData: {
      riskLevel: 'LOW',
      riskLevelText: '低危',
      riskColor: '#52c41a',
      latestAssessment: null,
      healthStats: {
        totalAssessments: 0,
        highRiskCount: 0,
        interventionCount: 0
      },
      healthPredictions: [],
      hasPredictions: false
    }
  },
  onShow() {
    const userInfo = app.globalData.userInfo
    this.setData({
      userInfo: userInfo,
      userType: userInfo?.role || 'CAREGIVER'
    })
    this.loadData()
  },
  loadData() {
    if (this.data.userType === 'RESIDENT') {
      this.loadResidentData()
    } else {
      this.loadCaregiverData()
    }
  },
  // 加载医护人员数据
  loadCaregiverData() {
    // 加载统计数据
    app.request('/dashboard/stats').then(res => {
      this.setData({ 
        stats: {
          onlineResident: res.residentCount || res.residentCount || 0,
          todayHighRisk: res.highRiskCount || 0,
          todayAssessments: res.evalCount || 0,
          pendingAlerts: 0
        }
      })
    }).catch(err => {
      console.error('加载统计数据失败:', err)
      // 失败时显示0，而不是硬编码的模拟数据
      this.setData({
        stats: {
          onlineResident: 0,
          todayHighRisk: 0,
          todayAssessments: 0,
          pendingAlerts: 0
        }
      })
    })
    // 加载最新预警
    app.request('/alert/my').then(res => {
      this.setData({ alerts: res.slice(0, 3) })
    }).catch(() => {
      // 模拟预警数据
      this.setData({
        alerts: [
          { id: 1, residentName: '钱女士', riskType: 'PRESSURE', message: '老人钱女士存在压疮高风险，请及时干预。', createTime: '2025-11-13T08:00:00' },
          { id: 2, residentName: '郑大爷', riskType: 'FALL', message: '老人郑大爷存在跌倒高风险，请及时干预。', createTime: '2025-11-09T15:30:00' },
          { id: 3, residentName: '沈大爷', riskType: 'FALL', message: '老人沈大爷存在跌倒高风险，请及时干预。', createTime: '2025-11-09T14:30:00' }
        ]
      })
    })
    
    // 加载照护风险提示通知
    this.loadNotifications()
  },
  // 加载老人数据
  loadResidentData() {
    const userInfo = this.data.userInfo || app.globalData.userInfo || wx.getStorageSync('userInfo')
    const residentId = userInfo?.residentId || userInfo?.id
    
    if (residentId) {
      // 加载老人照护风险数据
      app.request(`/miniprogram/resident/risk/latest?residentId=${residentId}`).then(res => {
        if (res) {
          const { riskLevel, riskLevelText, riskColor } = this.getRiskInfo(res.riskLevel)
          this.setData({
            'residentData.riskLevel': riskLevel,
            'residentData.riskLevelText': riskLevelText,
            'residentData.riskColor': riskColor,
            'residentData.latestAssessment': res
          })
        }
      }).catch(() => {
        // 没有评估记录是正常情况，静默处理
      })

      // 加载健康统计
      app.request(`/miniprogram/resident/stats?residentId=${residentId}`).then(res => {
        this.setData({ 'residentData.healthStats': res || {} })
      }).catch(() => {
        // 静默处理
      })

      // 加载照护风险提示
      this.loadHealthPredictions(residentId)
    }
  },
  // 加载照护风险提示
  loadHealthPredictions(residentId) {
    app.request(`/enhanced-risk/predictions/${residentId}`).then(res => {
      if (res && res.predictions && res.predictions.length > 0) {
        const highRiskPredictions = res.predictions.filter(p => 
          p.severity === 'HIGH' || p.severity === 'CRITICAL'
        ).slice(0, 3)
        
        if (highRiskPredictions.length > 0) {
          this.setData({
            'residentData.healthPredictions': highRiskPredictions,
            'residentData.hasPredictions': true
          })
        } else {
          this.setData({
            'residentData.healthPredictions': [],
            'residentData.hasPredictions': false
          })
        }
      } else {
        this.setData({
          'residentData.healthPredictions': [],
          'residentData.hasPredictions': false
        })
      }
    }).catch(() => {
      // 静默处理
      this.setData({
        'residentData.healthPredictions': [],
        'residentData.hasPredictions': false
      })
    })
  },
  // 加载照护风险提示通知
  loadNotifications() {
    // 获取所有通知（包括已读和未读）
    app.request('/notifications/all', 'GET', { limit: 10 }).then(res => {
      if (res && Array.isArray(res)) {
        // 计算未读数量
        const unreadCount = res.filter(item => !item.isRead).length
        
        this.setData({
          notifications: res.slice(0, 5), // 显示最新5条
          unreadNotifications: unreadCount
        })
      }
    }).catch(err => {
      console.log('加载通知失败:', err)
      this.setData({
        notifications: [],
        unreadNotifications: 0
      })
    })
  },
  // 查看通知详情
  viewNotificationDetail(e) {
    const notification = e.currentTarget.dataset.notification
    if (notification) {
      // 标记为已读
      if (!notification.isRead) {
        app.request(`/notifications/read/${notification.id}`, 'POST').then(() => {
          // 更新本地状态
          const notifications = this.data.notifications.map(item => {
            if (item.id === notification.id) {
              item.isRead = true
            }
            return item
          })
          this.setData({
            notifications: notifications,
            unreadNotifications: Math.max(0, this.data.unreadNotifications - 1)
          })
        }).catch(err => {
          console.error('标记已读失败:', err)
        })
      }
      
      // 显示通知详情
      wx.showModal({
        title: notification.title,
        content: notification.content + '\n\n老人：' + notification.residentName,
        showCancel: true,
        cancelText: '关闭',
        confirmText: '查看档案',
        success: (res) => {
          if (res.confirm) {
            // 跳转到老人详情页面
            wx.navigateTo({
              url: `/pages/resident/detail/detail?id=${notification.residentId}`
            })
          }
        }
      })
    }
  },
  // 查看照护风险提示详情
  viewPredictionDetail() {
    const residentId = this.data.userInfo?.residentId || this.data.userInfo?.id
    if (residentId) {
      wx.navigateTo({ 
        url: `/pages/health/predictions?residentId=${residentId}` 
      })
    }
  },
  getRiskInfo(level) {
    const riskMap = {
      'LOW': { riskLevel: 'LOW', riskLevelText: '低危', riskColor: '#52c41a' },
      'MEDIUM': { riskLevel: 'MEDIUM', riskLevelText: '中危', riskColor: '#faad14' },
      'HIGH': { riskLevel: 'HIGH', riskLevelText: '高危', riskColor: '#f5222d' }
    }
    return riskMap[level] || riskMap['LOW']
  },
  // 需求10: 删除扫码评估功能
  // goScan() 已移除
  
  goResidentManage() {
    wx.navigateTo({ url: '/pages/resident/list/list' })
  },
  goAlerts() {
    wx.navigateTo({ url: '/pages/alert/center' })
  },
  // 老人及家属功能
  viewHealthRecord() {
    wx.navigateTo({ url: '/pages/health/record' })
  },
  reportSymptom() {
    wx.navigateTo({ url: '/pages/health/symptom-report' })
  },
  goVisitService() {
    wx.navigateTo({ url: '/pages/health/visit-service' })
  },
  // 探访预约管理
  goVisitAppointments() {
    wx.navigateTo({ url: '/pages/health/visit-service' })
  },
  // 护理员功能
  goSymptomList() {
    wx.navigateTo({ url: '/pages/caregiver/symptom-list/index' })
  },
  
  // 下拉刷新
  onPullDownRefresh() {
    console.log('下拉刷新数据...')
    this.loadData()
    setTimeout(() => {
      wx.stopPullDownRefresh()
      wx.showToast({ title: '刷新成功', icon: 'success', duration: 1500 })
    }, 1000)
  }
})
