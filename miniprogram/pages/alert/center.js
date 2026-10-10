// 预警中心页面
const app = getApp()

Page({
  data: {
    alerts: [],
    filteredAlerts: [],
    loading: false,
    refreshing: false,
    filterType: 'ALL', // ALL, HIGH, MEDIUM, LOW
    stats: {
      total: 0,
      high: 0,
      medium: 0,
      low: 0
    }
  },

  onLoad() {
    this.loadAlerts()
  },

  onShow() {
    this.loadAlerts()
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({ refreshing: true, filterType: 'ALL' })
    this.loadAlerts()
    setTimeout(() => {
      wx.stopPullDownRefresh()
      this.setData({ refreshing: false })
    }, 1000)
  },

  // 加载预警数据
  loadAlerts() {
    this.setData({ loading: true })
    
    // 从后端API获取预警数据
    app.request('/alert/my', 'GET').then(res => {
      if (res && Array.isArray(res)) {
        // 转换数据格式
        const alerts = res.map(item => ({
          id: item.id,
          residentId: item.residentId, // 添加老人ID
          residentName: item.residentName || '未知',
          bedNo: item.bedNo || '-',
          careZone: item.careZone || '护理区待分配',
          riskType: item.riskType || 'FALL',
          riskLevel: item.riskLevel || 'MEDIUM',
          riskScore: item.riskScore || 0,
          alertTime: this.formatDateTime(item.createTime),
          description: item.message || '',
          status: item.status || 'PENDING',
          interventions: item.interventions ? item.interventions.split(',') : [],
          riskTypeText: this.getRiskTypeText(item.riskType),
          riskTypeIcon: this.getRiskTypeIcon(item.riskType)
        }))
        
        // 计算统计数据
        const stats = {
          total: alerts.length,
          high: alerts.filter(a => a.riskLevel === 'HIGH').length,
          medium: alerts.filter(a => a.riskLevel === 'MEDIUM').length,
          low: alerts.filter(a => a.riskLevel === 'LOW').length
        }
        
        this.setData({ 
          alerts: alerts,
          filteredAlerts: alerts,
          stats: stats,
          loading: false
        })
      } else {
        this.setData({ 
          alerts: [],
          filteredAlerts: [],
          stats: { total: 0, high: 0, medium: 0, low: 0 },
          loading: false
        })
      }
    }).catch(err => {
      console.error('加载预警数据失败:', err)
      this.setData({ 
        alerts: [],
        filteredAlerts: [],
        stats: { total: 0, high: 0, medium: 0, low: 0 },
        loading: false
      })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  formatDateTime(dateTime) {
    if (!dateTime) return ''
    
    try {
      const date = new Date(dateTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    } catch (e) {
      return dateTime
    }
  },

  // 筛选预警
  filterAlerts(e) {
    const filterType = e.currentTarget.dataset.type
    const { alerts } = this.data
    
    let filteredAlerts = alerts
    if (filterType !== 'ALL') {
      const levelMap = {
        'HIGH': 'HIGH',
        'MEDIUM': 'MEDIUM', 
        'LOW': 'LOW'
      }
      filteredAlerts = alerts.filter(alert => alert.riskLevel === levelMap[filterType])
    }
    
    this.setData({ 
      filterType,
      filteredAlerts
    })
  },

  // 处理预警
  handleAlert(e) {
    const alertId = e.currentTarget.dataset.id
    const alert = this.data.alerts.find(item => item.id === alertId)
    
    if (!alert) return
    
    wx.showActionSheet({
      itemList: ['标记为处理中', '标记为已完成', '查看详情', '添加干预措施'],
      success: (res) => {
        switch(res.tapIndex) {
          case 0:
            this.updateAlertStatus(alertId, 'PROCESSING')
            break
          case 1:
            this.updateAlertStatus(alertId, 'COMPLETED')
            break
          case 2:
            this.viewAlertDetail(alert)
            break
          case 3:
            this.addIntervention(alertId)
            break
        }
      }
    })
  },

  // 更新预警状态
  updateAlertStatus(alertId, status) {
    const alerts = this.data.alerts.map(alert => {
      if (alert.id === alertId) {
        alert.status = status
      }
      return alert
    })
    
    this.setData({ alerts })
    
    const statusText = {
      'PENDING': '待处理',
      'PROCESSING': '处理中',
      'COMPLETED': '已完成'
    }
    
    wx.showToast({
      title: `已标记为${statusText[status]}`,
      icon: 'success'
    })
  },

  // 查看预警详情
  viewAlertDetail(alert) {
    const interventionText = alert.interventions.join('、')
    
    wx.showModal({
      title: `${alert.residentName} - ${alert.bedNo}床`,
      content: `风险类型：${this.getRiskTypeText(alert.riskType)}\n风险等级：${alert.riskLevel}\n评估分数：${alert.riskScore}分\n建议措施：${interventionText}`,
      showCancel: false,
      confirmText: '知道了'
    })
  },

  // 添加干预措施
  addIntervention(alertId) {
    wx.showModal({
      title: '添加干预措施',
      content: '请输入新的干预措施',
      editable: true,
      placeholderText: '请输入干预措施',
      success: (res) => {
        if (res.confirm && res.content) {
          const alerts = this.data.alerts.map(alert => {
            if (alert.id === alertId) {
              alert.interventions.push(res.content)
            }
            return alert
          })
          
          this.setData({ alerts })
          wx.showToast({
            title: '添加成功',
            icon: 'success'
          })
        }
      }
    })
  },

  // 获取风险类型文本
  getRiskTypeText(type) {
    const typeMap = {
      'FALL': '跌倒风险',
      'PRESSURE': '压疮风险',
      'COGNITIVE': '认知障碍风险',
      'MALNUTRITION': '营养不良风险',
      'DELIRIUM': '谵妄风险',
      'ABUSE': '虐待忽视风险',
      'SELF_HARM': '自伤风险',
      'DEPRESSION': '抑郁风险'
    }
    return typeMap[type] || '综合照护风险'
  },

  getRiskTypeIcon(type) {
    const iconMap = {
      FALL: '行', PRESSURE: '护', COGNITIVE: '认', MALNUTRITION: '养',
      DELIRIUM: '察', ABUSE: '安', SELF_HARM: '心', DEPRESSION: '情'
    }
    return iconMap[type] || '警'
  },

  // 跳转到老人详情
  goToResidentDetail(e) {
    const alertId = e.currentTarget.dataset.id
    const alert = this.data.alerts.find(item => item.id === alertId)
    
    if (!alert) {
      wx.showToast({
        title: '预警信息不存在',
        icon: 'none'
      })
      return
    }

    if (alert.residentId) {
      // 有老人ID，直接跳转
      wx.navigateTo({
        url: `/pages/resident/detail/detail?id=${alert.residentId}`
      })
    } else if (alert.residentName) {
      // 没有老人ID，尝试通过老人姓名查找
      wx.showLoading({ title: '查找老人...' })
      
      app.request(`/resident/list?name=${encodeURIComponent(alert.residentName)}&current=1&size=10`, 'GET').then(res => {
        wx.hideLoading()
        
        if (res && res.records && res.records.length > 0) {
          const resident = res.records[0]
          wx.navigateTo({
            url: `/pages/resident/detail/detail?id=${resident.id}`
          })
        } else {
          wx.showModal({
            title: '提示',
            content: `未找到老人“${alert.residentName}”的详细信息，可能该老人已离院或档案数据不完整。`,
            showCancel: false
          })
        }
      }).catch(err => {
        wx.hideLoading()
        wx.showToast({
          title: '查找失败',
          icon: 'none'
        })
      })
    } else {
      wx.showModal({
        title: '提示',
        content: '该预警记录缺少老人信息，请联系管理员更新数据。',
        showCancel: false
      })
    }
  }
})
