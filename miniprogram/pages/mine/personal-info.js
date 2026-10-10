const app = getApp()

Page({
  data: {
    userInfo: {}
  },

  onLoad() {
    this.loadUserInfo()
  },

  loadUserInfo() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    
    // 如果有老人ID，从后端获取完整档案
    if (userInfo.residentId || userInfo.id) {
      const residentId = userInfo.residentId || userInfo.id
      
      wx.showLoading({ title: '加载中...' })
      
      app.request(`/resident/${residentId}`, 'GET').then(res => {
        wx.hideLoading()
        
        // 合并本地和服务器数据
        const completeUserInfo = app.normalizeUser({
          ...userInfo,
          ...res,
          id: res.id || userInfo.id,
          name: res.name || userInfo.name,
          residentId: res.residentId || userInfo.residentId,
          roomBedNo: res.roomBedNo || userInfo.roomBedNo,
          age: res.age || userInfo.age,
          gender: res.gender || userInfo.gender,
          careZone: res.careZone || userInfo.careZone,
          phone: res.phone || userInfo.phone,
          idCard: res.idCard || userInfo.idCard,
          admitTime: res.admitTime || userInfo.admitTime,
          emergencyContactName: res.emergencyContactName || userInfo.emergencyContactName,
          emergencyContactPhone: res.emergencyContactPhone || userInfo.emergencyContactPhone
        })
        
        this.setData({ userInfo: completeUserInfo })
      }).catch(err => {
        wx.hideLoading()
        console.error('获取老人信息失败:', err)
        // 使用本地缓存的信息
        this.setData({ userInfo })
      })
    } else {
      // 没有老人ID，使用本地信息
      this.setData({ userInfo })
    }
  },

  formatDate(dateStr) {
    if (!dateStr) return '-'
    
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
  }
})
