App({
  globalData: {
    userInfo: null,
    // 请将此IP改为您电脑的局域网IP，真机调试需要
    baseUrl: 'http://localhost:8080/api' 
  },
  onLaunch() {
    const userInfo = wx.getStorageSync('userInfo')
    if (userInfo) {
      this.globalData.userInfo = this.normalizeUser(userInfo)
    }
  },
  // 标准化用户信息（员工/老人通用）
  normalizeUser(user = {}) {
    return {
      ...user,
      // 员工字段兼容
      workZone: user.workZone || user.work_zone || user.careZone || '',
      // 老人字段兼容（如果是老人账号）
      residentId: user.residentId || user.id,
      residentNo: user.residentNo || '',
      roomBedNo: user.roomBedNo || user.bedNo,
      bedNo: user.roomBedNo || user.bedNo,
      careZone: user.careZone || '',
      healthStatus: user.healthStatus || '',
      admitTime: user.admitTime || '',
      emergencyContactPhone: user.emergencyContactPhone || user.emergencyPhone
    }
  },
  // 封装请求方法
  request(url, method, data) {
    return new Promise((resolve, reject) => {
      const userInfo = this.globalData.userInfo || wx.getStorageSync('userInfo') || {}
      const token = wx.getStorageSync('token') || ''
      
      let requestUrl = this.globalData.baseUrl + url
      let requestData = data || {}
      
      if (method === 'GET' && url.indexOf('?') > -1) {
        requestData = {}
      }
      
      wx.request({
        url: requestUrl,
        method: method || 'GET',
        data: requestData,
        header: {
          'content-type': 'application/json',
          'Authorization': token,
          'User-Role': userInfo.role || 'RESIDENT'
        },
        success: (res) => {
          if (res.data.code === 200) {
            resolve(res.data.data)
          } else {
            const msg = res.data.msg || res.data.message || '请求失败'
            reject({ ...res.data, message: msg })
          }
        },
        fail: (err) => {
          wx.showToast({ title: '网络连接失败', icon: 'none' })
          reject(err)
        }
      })
    })
  }
})
