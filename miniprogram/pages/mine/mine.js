const app = getApp()

Page({
  data: {
    userInfo: {},
    displayName: '未知用户',
    avatarText: '未',
    roleText: '用户',
    zoneText: ''
  },
  onShow() {
    const userInfo = app.globalData.userInfo || {}
    this.setData({ userInfo })
    this.updateDisplayData(userInfo)
  },
  updateDisplayData(userInfo) {
    const displayName = userInfo.realName || userInfo.name || '未知用户'
    const avatarText = displayName.charAt(0)
    
    const roleMap = {
      'ADMIN': '管理员',
      'DIRECTOR': '照护主管',
      'CAREGIVER': '护理员',
      'CAREGIVER': '护理员',
      'RESIDENT': '老人'
    }
    const roleText = roleMap[userInfo.role] || '员工'
    const zoneText = userInfo.careZone || ''
    
    this.setData({ displayName, avatarText, roleText, zoneText })
  },
  
  changePassword() {
    wx.navigateTo({ url: '/pages/mine/change-password' })
  },
  
  aboutSystem() {
    wx.showModal({
      title: '关于系统',
      content: '颐康云·智慧养老服务平台 v2.0\n\n为入住老人提供专业化的照护服务。',
      showCancel: false
    })
  },
  
  personalInfo() {
    const userInfo = this.data.userInfo
    if (userInfo.role === 'RESIDENT') {
      wx.navigateTo({ url: '/pages/mine/personal-info' })
    } else {
      wx.navigateTo({ url: '/pages/mine/edit-profile' })
    }
  },
  
  viewAssessmentReports() {
    // 必须传数字 id（数据库主键），不能传 residentId（字符串）
    const userId = this.data.userInfo.id
    if (userId) {
      wx.navigateTo({
        url: `/pages/assessment-reports/assessment-reports?userId=${userId}`
      })
    } else {
      wx.showToast({ title: '无法获取用户信息', icon: 'none' })
    }
  },
  
  logout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.clearStorageSync()
          app.globalData.userInfo = null
          wx.reLaunch({ url: '/pages/login/login' })
        }
      }
    })
  }
})

