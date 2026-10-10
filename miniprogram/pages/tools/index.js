// 养老照护工具页面
const app = getApp()

Page({
  data: {
    userInfo: {},
    userType: 'CAREGIVER',
    tools: [],
    // 照护主管工具
    careManagerTools: [
      {
        id: 'resident-manage',
        name: '老人管理',
        desc: '查看和管理老人档案',
        icon: '👥',
        color: '#007bff',
        url: '/pages/resident/list/list'
      },
      {
        id: 'appointments',
        name: '探访管理',
        desc: '管理家属探访预约',
        icon: '📅',
        color: '#17a2b8',
        url: '/pages/health/visit-service'
      },
      {
        id: 'alert-center',
        name: '预警中心',
        desc: '查看高风险老人预警',
        icon: '🔔',
        color: '#dc3545',
        url: '/pages/alert/center'
      },
      {
        id: 'statistics',
        name: '统计报表',
        desc: '查看各类统计数据',
        icon: '📊',
        color: '#ffc107',
        url: '/pages/statistics/report'
      },
      {
        id: 'knowledge',
        name: '知识库',
        desc: '查看养老照护知识和指南',
        icon: '📚',
        color: '#fd7e14',
        url: '/pages/knowledge/base'
      }
    ],
    // 护理员/照护员工具
    caregiverTools: [
      {
        id: 'resident-manage',
        name: '老人管理',
        desc: '查看和管理老人档案',
        icon: '👥',
        color: '#007bff',
        url: '/pages/resident/list/list'
      },
      {
        id: 'vital-signs',
        name: '生命体征',
        desc: '记录老人生命体征',
        icon: '🩺',
        color: '#20c997',
        url: '/pages/caregiver/vital-signs/index'
      },
      {
        id: 'medication-manage',
        name: '用药管理',
        desc: '管理老人用药计划',
        icon: '💊',
        color: '#6f42c1',
        url: '/pages/caregiver/medication/index'
      },
      {
        id: 'alert-center',
        name: '预警中心',
        desc: '查看高风险老人预警',
        icon: '🔔',
        color: '#dc3545',
        url: '/pages/alert/center'
      },
      {
        id: 'bed-swap',
        name: '床位调换',
        desc: '调换老人房间床位',
        icon: '🛏️',
        color: '#17a2b8',
        url: '/pages/caregiver/bed-swap/index'
      },
      {
        id: 'care-plan',
        name: '护理计划',
        desc: '制定和执行护理计划',
        icon: '📋',
        color: '#28a745',
        url: '/pages/care/plan'
      },
      {
        id: 'statistics',
        name: '统计报表',
        desc: '查看各类统计数据',
        icon: '📊',
        color: '#ffc107',
        url: '/pages/statistics/report'
      },
      {
        id: 'knowledge',
        name: '知识库',
        desc: '查看护理知识和指南',
        icon: '📚',
        color: '#fd7e14',
        url: '/pages/knowledge/base'
      }
    ],
    residentTools: [
      {
        id: 'health-record',
        name: '健康档案',
        desc: '查看我的健康档案',
        icon: '📋',
        color: '#007bff',
        url: '/pages/health/record'
      },
      {
        id: 'risk-history',
        name: '评估记录',
        desc: '查看风险评估历史',
        icon: '📊',
        color: '#28a745',
        url: '/pages/health/record'
      },
      {
        id: 'symptom-report',
        name: '症状上报',
        desc: '上报身体不适症状',
        icon: '🩺',
        color: '#ffc107',
        url: '/pages/health/symptom-report'
      },
      {
        id: 'medication',
        name: '用药提醒',
        desc: '查看用药计划',
        icon: '💊',
        color: '#6f42c1',
        url: '/pages/health/medication'
      },
      {
        id: 'appointment',
        name: '探访预约',
        desc: '提交家属探访申请',
        icon: '📅',
        color: '#fd7e14',
        url: '/pages/health/visit-service'
      }
    ]
  },

  onLoad() {
    this.loadUserInfo()
  },

  onShow() {
    this.loadUserInfo()
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo')
    if (userInfo) {
      const userType = userInfo.role || 'CAREGIVER'
      let tools = []
      
      // 需求13: 根据角色显示不同工具
      if (userType === 'RESIDENT' || userType === 'RESIDENT') {
        tools = this.data.residentTools
      } else if (userType === 'CARE_MANAGER' || userType === 'DIRECTOR') {
        tools = this.data.careManagerTools
      } else {
        tools = this.data.caregiverTools
      }
      
      this.setData({ 
        userInfo,
        userType,
        tools
      })
    } else {
      // 默认显示护理员工具
      this.setData({ tools: this.data.caregiverTools })
    }
  },

  // 跳转到工具页面
  navigateToTool(e) {
    const toolId = e.currentTarget.dataset.id
    const tool = this.data.tools.find(item => item.id === toolId)
    
    if (tool && tool.url) {
      wx.navigateTo({
        url: tool.url,
        fail: (err) => {
          console.error('导航失败:', err)
          wx.showToast({
            title: '页面跳转失败',
            icon: 'none'
          })
        }
      })
    }
  }
})
