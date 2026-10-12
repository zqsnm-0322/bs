const app = getApp()

const STATUS_TEXT = {
  PENDING: '待确认', APPROVED: '已批准', REJECTED: '已拒绝',
  CANCELLED: '已取消', COMPLETED: '已完成'
}

Page({
  data: {
    userInfo: {}, appointments: [], activeTab: 'book', visitorName: '', visitorPhone: '',
    visitorRelation: '', visitorCount: 1, selectedDate: '', startTime: '09:00',
    endTime: '10:00', specialNotes: '', submitting: false, loading: false,
    minDate: '', relationOptions: ['子女', '配偶', '兄弟姐妹', '亲属', '其他']
  },

  onLoad() {
    const userInfo = app.normalizeUser(app.globalData.userInfo || wx.getStorageSync('userInfo') || {})
    this.setData({ userInfo })
    this.initDate()
  },

  onShow() {
    if (this.data.activeTab === 'history') this.loadAppointments()
  },

  initDate() {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    const year = tomorrow.getFullYear()
    const month = String(tomorrow.getMonth() + 1).padStart(2, '0')
    const day = String(tomorrow.getDate()).padStart(2, '0')
    const selectedDate = `${year}-${month}-${day}`
    const today = new Date()
    const todayValue = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
    this.setData({ selectedDate, minDate: todayValue })
  },

  switchTab(e) {
    const activeTab = e.currentTarget.dataset.tab
    this.setData({ activeTab })
    if (activeTab === 'history') this.loadAppointments()
  },

  onInput(e) { this.setData({ [e.currentTarget.dataset.field]: e.detail.value }) },
  onDateChange(e) { this.setData({ selectedDate: e.detail.value }) },
  onStartTimeChange(e) { this.setData({ startTime: e.detail.value }) },
  onEndTimeChange(e) { this.setData({ endTime: e.detail.value }) },
  selectRelation(e) { this.setData({ visitorRelation: e.currentTarget.dataset.value }) },
  changeCount(e) {
    const next = Number(this.data.visitorCount) + Number(e.currentTarget.dataset.delta)
    this.setData({ visitorCount: Math.min(10, Math.max(1, next)) })
  },

  loadAppointments() {
    const residentId = this.data.userInfo.id || this.data.userInfo.residentId
    if (!residentId) return
    this.setData({ loading: true })
    app.request(`/visit/list?current=1&size=50&residentId=${residentId}`, 'GET')
      .then(res => {
        const appointments = (res.records || []).map(item => ({
          ...item,
          visitStartTime: item.visitStartTime ? item.visitStartTime.slice(0, 5) : '',
          visitEndTime: item.visitEndTime ? item.visitEndTime.slice(0, 5) : '',
          statusText: STATUS_TEXT[item.status] || item.status
        }))
        this.setData({ appointments })
      })
      .catch(() => this.setData({ appointments: [] }))
      .finally(() => this.setData({ loading: false }))
  },

  validate() {
    if (!(this.data.userInfo.id || this.data.userInfo.residentId)) return '未获取到老人信息'
    if (!this.data.visitorName.trim()) return '请输入探访人姓名'
    if (!this.data.visitorPhone.trim()) return '请输入探访人电话'
    if (!/^1[3-9]\d{9}$/.test(this.data.visitorPhone.trim())) return '请输入正确的手机号码'
    if (!this.data.selectedDate) return '请选择探访日期'
    if (this.data.startTime >= this.data.endTime) return '结束时间必须晚于开始时间'
    return ''
  },

  submitAppointment() {
    const error = this.validate()
    if (error) return wx.showToast({ title: error, icon: 'none' })
    const data = {
      residentId: this.data.userInfo.id || this.data.userInfo.residentId,
      visitorName: this.data.visitorName.trim(), visitorPhone: this.data.visitorPhone.trim(),
      visitorRelation: this.data.visitorRelation.trim(), visitorCount: Number(this.data.visitorCount) || 1,
      visitDate: this.data.selectedDate, visitStartTime: this.data.startTime,
      visitEndTime: this.data.endTime, specialNotes: this.data.specialNotes.trim()
    }
    this.setData({ submitting: true })
    app.request('/visit', 'POST', data)
      .then(() => {
        wx.showToast({ title: '预约已提交', icon: 'success' })
        this.setData({ activeTab: 'history', visitorName: '', visitorPhone: '', visitorRelation: '', visitorCount: 1, specialNotes: '' })
        this.loadAppointments()
      })
      .catch(err => wx.showToast({ title: err.message || err.msg || '提交失败', icon: 'none' }))
      .finally(() => this.setData({ submitting: false }))
  },

  cancelAppointment(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({ title: '取消探访预约', content: '确定取消这条待确认预约吗？', success: res => {
      if (!res.confirm) return
      app.request(`/visit/cancel/${id}`, 'POST', {}).then(() => {
        wx.showToast({ title: '已取消', icon: 'success' })
        this.loadAppointments()
      })
    } })
  },

  onPullDownRefresh() {
    this.loadAppointments()
    setTimeout(() => wx.stopPullDownRefresh(), 800)
  }
})
