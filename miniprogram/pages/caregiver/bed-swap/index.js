// 床位调换页面
const app = getApp()

Page({
  data: {
    residents: [],
    selectedResident: null,
    targetBedNo: '',
    availableBeds: [], // 可用床位列表
    careZones: ['自理区', '半护区', '全护区', '失智照护区'],
    selectedCareZone: '',
    loading: false,
    searchKeyword: ''
  },

  onLoad() {
    this.loadResidents()
  },

  // 加载老人列表
  loadResidents() {
    wx.showLoading({ title: '加载中...' })
    
    wx.request({
      url: `${app.globalData.baseUrl}/resident/list`,
      method: 'GET',
      data: {
        current: 1,
        size: 100
      },
      success: (res) => {
        if (res.data.code === 200) {
          const residents = res.data.data.records.map(app.normalizeUser).filter(item => item.roomBedNo)
          this.setData({ residents })
        } else {
          wx.showToast({
            title: res.data.message || '加载失败',
            icon: 'none'
          })
        }
      },
      fail: () => {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  },

  // 选择老人
  selectResident(e) {
    const index = e.currentTarget.dataset.index
    const resident = this.data.residents[index]
    
    this.setData({
      selectedResident: resident,
      selectedCareZone: resident.careZone || '',
      targetBedNo: '',
      availableBeds: []
    })
    
    // 自动加载该护理区的床位
    if (resident.careZone) {
      this.loadAvailableBeds(resident.careZone)
    }
  },

  // 选择护理区
  onCareZoneChange(e) {
    const careZone = this.data.careZones[e.detail.value]
    this.setData({
      selectedCareZone: careZone,
      targetBedNo: '',
      availableBeds: []
    })
    // 加载该护理区的床位列表
    this.loadAvailableBeds(careZone)
  },

  // 加载可用床位列表
  loadAvailableBeds(careZone) {
    if (!careZone) return

    wx.showLoading({ title: '加载床位...' })
    
    wx.request({
      url: `${app.globalData.baseUrl}/bed/list`,
      method: 'GET',
      data: {
        current: 1,
        size: 100,
        careZone
      },
      success: (res) => {
        if (res.data.code === 200) {
          // 获取所有床位（包括空闲和已占用的）
          const beds = res.data.data.records || []
          this.setData({ availableBeds: beds })
        } else {
          wx.showToast({
            title: res.data.message || '加载床位失败',
            icon: 'none'
          })
        }
      },
      fail: () => {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  },

  // 选择目标床位
  onBedChange(e) {
    const index = e.detail.value
    const bed = this.data.availableBeds[index]
    this.setData({
      targetBedNo: bed.roomBedNo
    })
  },

  // 搜索老人
  onSearchInput(e) {
    this.setData({
      searchKeyword: e.detail.value
    })
  },

  // 执行床位调换
  swapBed() {
    const { selectedResident, targetBedNo, selectedCareZone } = this.data

    if (!selectedResident) {
      wx.showToast({
        title: '请选择老人',
        icon: 'none'
      })
      return
    }

    if (!targetBedNo || !targetBedNo.trim()) {
      wx.showToast({
        title: '请输入目标床位号',
        icon: 'none'
      })
      return
    }

    if (!selectedCareZone) {
      wx.showToast({
        title: '请选择护理区',
        icon: 'none'
      })
      return
    }

    if (selectedResident.bedNo === targetBedNo.trim()) {
      wx.showToast({
        title: '目标床位与当前床位相同',
        icon: 'none'
      })
      return
    }

    wx.showModal({
      title: '确认调换',
      content: `确定将 ${selectedResident.name} 从 ${selectedResident.bedNo} 调换到 ${targetBedNo.trim()} 吗？`,
      success: (res) => {
        if (res.confirm) {
          this.performSwap()
        }
      }
    })
  },

  // 执行调换请求
  performSwap() {
    const { selectedResident, targetBedNo, selectedCareZone } = this.data

    this.setData({ loading: true })
    wx.showLoading({ title: '调换中...' })

    wx.request({
      url: `${app.globalData.baseUrl}/bed/swap`,
      method: 'POST',
      data: {
        residentId1: selectedResident.id,
        roomBedNo1: selectedResident.roomBedNo,
        roomBedNo2: targetBedNo.trim(),
        careZone: selectedCareZone
      },
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({
            title: '床位调换成功',
            icon: 'success'
          })
          
          // 重置表单
          this.setData({
            selectedResident: null,
            targetBedNo: '',
            selectedCareZone: ''
          })
          
          // 重新加载老人列表
          setTimeout(() => {
            this.loadResidents()
          }, 1500)
        } else {
          wx.showToast({
            title: res.data.message || '调换失败',
            icon: 'none',
            duration: 2000
          })
        }
      },
      fail: () => {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
      },
      complete: () => {
        this.setData({ loading: false })
        wx.hideLoading()
      }
    })
  },

  // 取消选择
  cancelSelection() {
    this.setData({
      selectedResident: null,
      targetBedNo: '',
      selectedCareZone: ''
    })
  }
})
