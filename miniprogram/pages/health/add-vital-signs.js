const app = getApp()

Page({
  data: {
    formData: {
      recordDate: '',
      recordTime: '',
      temperature: '',
      systolicPressure: '',
      diastolicPressure: '',
      heartRate: '',
      respiratoryRate: '',
      bloodOxygen: '',
      weight: '',
      bloodGlucose: '',
      remark: ''
    }
  },

  onLoad() {
    // 设置默认日期和时间为当前
    const now = new Date()
    const dateStr = this.formatDate(now)
    const timeStr = this.formatTime(now)
    
    this.setData({
      'formData.recordDate': dateStr,
      'formData.recordTime': timeStr
    })
  },

  formatDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  formatTime(date) {
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes}`
  },

  onDateChange(e) {
    this.setData({
      'formData.recordDate': e.detail.value
    })
  },

  onTimeChange(e) {
    this.setData({
      'formData.recordTime': e.detail.value
    })
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    this.setData({
      [`formData.${field}`]: value
    })
  },

  validateForm() {
    const { recordDate, recordTime } = this.data.formData
    
    if (!recordDate) {
      wx.showToast({ title: '请选择记录日期', icon: 'none' })
      return false
    }
    
    if (!recordTime) {
      wx.showToast({ title: '请选择记录时间', icon: 'none' })
      return false
    }
    
    // 至少需要填写一项生命体征数据
    const { temperature, systolicPressure, diastolicPressure, heartRate, respiratoryRate, bloodOxygen, weight, bloodGlucose } = this.data.formData
    
    if (!temperature && !systolicPressure && !diastolicPressure && !heartRate && !respiratoryRate && !bloodOxygen && !weight && !bloodGlucose) {
      wx.showToast({ title: '请至少填写一项生命体征数据', icon: 'none' })
      return false
    }
    
    return true
  },

  onSubmit() {
    if (!this.validateForm()) {
      return
    }

    const userInfo = app.globalData.userInfo || {}
    const residentId = userInfo.residentId || userInfo.id

    if (!residentId) {
      wx.showToast({ title: '老人信息缺失', icon: 'none' })
      return
    }

    wx.showLoading({ title: '保存中...', mask: true })

    // 构建提交数据
    const vitalSignsData = {
      residentId: residentId,
      residentName: userInfo.name || '老人',
      bedNo: userInfo.bedNo,
      careZone: userInfo.careZone,
      recordDate: this.data.formData.recordDate,
      recordTime: this.data.formData.recordTime,
      temperature: this.data.formData.temperature || null,
      systolicPressure: this.data.formData.systolicPressure || null,
      diastolicPressure: this.data.formData.diastolicPressure || null,
      heartRate: this.data.formData.heartRate || null,
      respiratoryRate: this.data.formData.respiratoryRate || null,
      bloodOxygen: this.data.formData.bloodOxygen || null,
      weight: this.data.formData.weight || null,
      bloodGlucose: this.data.formData.bloodGlucose || null,
      remark: this.data.formData.remark,
      recorderName: userInfo.name || '老人',
      recorderType: 'RESIDENT'
    }

    // 调用后端API保存数据
    app.request('/vital-signs', 'POST', vitalSignsData).then(res => {
      wx.hideLoading()
      wx.showToast({
        title: '保存成功',
        icon: 'success',
        duration: 2000
      })

      // 返回上一页并刷新
      setTimeout(() => {
        wx.navigateBack({
          success: () => {
            const pages = getCurrentPages()
            const prevPage = pages[pages.length - 2]
            if (prevPage && prevPage.loadVitalSigns) {
              prevPage.loadVitalSigns()
            }
          }
        })
      }, 2000)
    }).catch(err => {
      wx.hideLoading()
      console.error('保存生命体征失败:', err)
      wx.showToast({
        title: '保存失败',
        icon: 'none'
      })
    })
  },

  onCancel() {
    wx.navigateBack()
  }
})
