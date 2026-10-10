const app = getApp()

Page({
  data: {
    list: [],
    userType: 'CAREGIVER',
    isResident: false
  },
  onShow() {
    const userInfo = app.globalData.userInfo
    const isResident = userInfo?.role === 'RESIDENT'
    
    this.setData({ 
      userType: userInfo?.role || 'CAREGIVER',
      isResident: isResident
    })
    
    if (isResident) {
      // 入住老人账号只可查看自己的信息
      this.showResidentTip()
    } else {
      // 员工正常加载老人列表
      this.fetchList()
    }
  },
  showResidentTip() {
    wx.showModal({
      title: '提示',
      content: '此功能仅供颐康园员工使用。入住老人可在“首页”查看个人健康信息。',
      showCancel: false,
      success: () => {
        wx.switchTab({ url: '/pages/index/index' })
      }
    })
  },
  fetchList(keyword = '') {
    app.request(`/resident/list?current=1&size=100&name=${encodeURIComponent(keyword)}`).then(res => {
      this.setData({ list: (res.records || []).map(app.normalizeUser) })
    }).catch(err => {
      console.error('获取老人列表失败:', err)
      this.setData({ list: [] })
    })
  },
  onSearch(e) {
    this.fetchList(e.detail.value)
  },
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/resident/detail/detail?id=${id}` })
  },
  goAssess(e) {
    const id = e.currentTarget.dataset.id
    const resident = this.data.list.find(p => p.id === id)
    const residentName = resident ? resident.name : ''
    wx.navigateTo({ url: `/pages/risk/assess?residentId=${id}&residentName=${residentName}` })
  },
  // 老人用户的功能方法
  goHome() {
    wx.switchTab({ url: '/pages/index/index' })
  },
  viewHealth() {
    wx.showToast({ title: '健康档案功能开发中', icon: 'none' })
  }
})
