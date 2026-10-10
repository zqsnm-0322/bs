const app = getApp()

Page({
  data: {
    loading: true,
    updatedAt: '',
    stats: {
      residentCount: 0,
      bedOccupancyRate: 0,
      newAdmissions: 0,
      leavingCount: 0,
      pendingVisits: 0,
      highRiskCount: 0
    },
    riskDistribution: [],
    zoneDistribution: [],
    healthLevelDistribution: []
  },

  onLoad() { this.loadStatistics() },
  onShow() { this.loadStatistics() },

  loadStatistics() {
    if (this.data.loading !== true) this.setData({ loading: true })
    Promise.all([
      app.request('/dashboard/stats', 'GET'),
      app.request('/statistics/risk-distribution', 'GET'),
      app.request('/visit/list?current=1&size=1&status=PENDING', 'GET')
    ]).then(([dashboard, risks, visits]) => {
      const riskDistribution = this.buildDistribution([
        { name: '高危', value: Number(risks.HIGH || 0), color: '#ef6262' },
        { name: '中危', value: Number(risks.MEDIUM || 0), color: '#f2b84b' },
        { name: '低危', value: Number(risks.LOW || 0), color: '#2ab68a' }
      ])
      this.setData({
        stats: {
          residentCount: Number(dashboard.residentCount || 0),
          bedOccupancyRate: Number(dashboard.bedOccupancyRate || 0),
          newAdmissions: Number(dashboard.newAdmissions || 0),
          leavingCount: Number(dashboard.leavingCount || 0),
          pendingVisits: Number(visits.total || 0),
          highRiskCount: Number(dashboard.highRiskCount || 0)
        },
        riskDistribution,
        zoneDistribution: this.buildDistribution(dashboard.zoneDistribution || []),
        healthLevelDistribution: this.buildDistribution(dashboard.healthLevelDistribution || []),
        updatedAt: this.formatNow(),
        loading: false
      })
    }).catch(err => {
      console.error('加载养老运营统计失败:', err)
      this.setData({ loading: false })
      wx.showToast({ title: '统计数据加载失败', icon: 'none' })
    }).finally(() => wx.stopPullDownRefresh())
  },

  buildDistribution(items) {
    const colors = ['#24b78b', '#5b86e5', '#f0ad45', '#9b71d1']
    const normalized = items.map((item, index) => ({
      name: item.name,
      value: Number(item.value || 0),
      color: item.color || colors[index % colors.length]
    }))
    const total = normalized.reduce((sum, item) => sum + item.value, 0)
    return normalized.map(item => ({
      ...item,
      percent: total > 0 ? Math.round(item.value * 100 / total) : 0
    }))
  },

  formatNow() {
    const now = new Date()
    return `${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  },

  goResidents() { wx.navigateTo({ url: '/pages/resident/list/list' }) },
  goVisits() { wx.navigateTo({ url: '/pages/health/visit-service' }) },
  goAlerts() { wx.navigateTo({ url: '/pages/alert/center' }) },
  onPullDownRefresh() { this.loadStatistics() }
})
