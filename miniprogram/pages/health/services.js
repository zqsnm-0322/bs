const app = getApp()

Page({
  data: {
    userInfo: {},
    userType: 'CAREGIVER',
    isResident: false,
    residentServices: [
      {
        id: 'health-record',
        icon: '📋',
        title: '健康档案',
        desc: '查看个人健康记录和评估历史',
        color: '#667eea'
      },
      {
        id: 'symptom-report',
        icon: '📝',
        title: '症状上报',
        desc: '报告身体不适和症状变化',
        color: '#4facfe'
      },
      {
        id: 'medication-reminder',
        icon: '💊',
        title: '用药提醒',
        desc: '查看用药计划和提醒设置',
        color: '#43e97b'
      },
      {
        id: 'appointment',
        icon: '📅',
        title: '探访预约',
        desc: '提交并查看家属探访申请',
        color: '#ffa726'
      },
      {
        id: 'consultation',
        icon: '💬',
        title: '照护沟通',
        desc: '与照护人员在线交流',
        color: '#ab47bc'
      }
    ],
    caregiverServices: [
      {
        id: 'resident-list',
        icon: '👥',
        title: '老人管理',
        desc: '查看和管理老人信息',
        color: '#667eea'
      },
      {
        id: 'risk-assessment',
        icon: '⚠️',
        title: '风险评估',
        desc: '进行老人风险评估',
        color: '#f093fb'
      },
      {
        id: 'alert-center',
        icon: '🔔',
        title: '预警中心',
        desc: '查看高危老人预警',
        color: '#ff6b6b'
      },
      {
        id: 'nursing-plan',
        icon: '📋',
        title: '护理计划',
        desc: '制定和执行护理计划',
        color: '#4facfe'
      },
      {
        id: 'statistics',
        icon: '📊',
        title: '统计报表',
        desc: '查看护理区统计数据',
        color: '#43e97b'
      },
      {
        id: 'knowledge-base',
        icon: '📖',
        title: '知识库',
        desc: '查看护理知识和指南',
        color: '#ffa726'
      }
    ]
  },

  onShow() {
    const userInfo = app.globalData.userInfo || {}
    const isResident = userInfo.role === 'RESIDENT'
    
    this.setData({
      userInfo,
      userType: userInfo.role || 'CAREGIVER',
      isResident
    })
  },

  // 服务项点击事件
  onServiceTap(e) {
    const serviceId = e.currentTarget.dataset.id
    
    switch(serviceId) {
      case 'health-record':
        this.goHealthRecord()
        break
      case 'symptom-report':
        this.goSymptomReport()
        break
      case 'medication-reminder':
        this.goMedicationReminder()
        break
      case 'appointment':
        this.goVisitService()
        break
      case 'consultation':
        this.goCareConsultation()
        break
      case 'resident-list':
        wx.navigateTo({ url: '/pages/resident/list/list' })
        break
      case 'risk-assessment':
        wx.navigateTo({ url: '/pages/risk/assess' })
        break
      case 'alert-center':
        this.goAlertCenter()
        break
      case 'nursing-plan':
        this.goNursingPlan()
        break
      case 'statistics':
        this.goStatistics()
        break
      case 'knowledge-base':
        this.goKnowledgeBase()
        break
      default:
        wx.showToast({ title: '功能开发中', icon: 'none' })
    }
  },

  // 老人功能方法
  goHealthRecord() {
    wx.navigateTo({ url: '/pages/health/record' })
  },

  goSymptomReport() {
    wx.navigateTo({ url: '/pages/health/symptom-report' })
  },

  goMedicationReminder() {
    wx.navigateTo({ url: '/pages/health/medication' })
  },

  goVisitService() {
    wx.navigateTo({ url: '/pages/health/visit-service' })
  },

  goCareConsultation() {
    wx.navigateTo({ url: '/pages/health/care-consultation' })
  },

  // 医护功能方法
  goAlertCenter() {
    wx.navigateTo({ url: '/pages/alert/center' })
  },

  goNursingPlan() {
    wx.navigateTo({ url: '/pages/care/plan' })
  },

  goStatistics() {
    wx.navigateTo({ url: '/pages/statistics/report' })
  },

  goKnowledgeBase() {
    wx.navigateTo({ url: '/pages/knowledge/base' })
  }
})
