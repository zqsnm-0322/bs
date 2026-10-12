const app = getApp()

Page({
    data: {
        userInfo: {},
        reports: [],
        pendingCount: 0,
        activeFilter: 'all',
        keyword: '',
        loading: false,
        pageNum: 1,
        total: 0
    },

    onLoad() {
        const userInfo = app.globalData.userInfo || {}
        this.setData({ userInfo })
        this.loadReports()
    },

    onShow() {
        this.loadReports()
    },

    // 加载症状上报列表
    loadReports() {
        this.setData({ loading: true })

        let url = `/symptom/list?pageNum=${this.data.pageNum}&pageSize=20`
        if (this.data.activeFilter !== 'all') {
            url += `&status=${this.data.activeFilter}`
        }
        if (this.data.keyword) {
            url += `&keyword=${encodeURIComponent(this.data.keyword)}`
        }

        app.request(url).then(res => {
            this.setData({
                reports: res.list || [],
                total: res.total || 0,
                pendingCount: res.pendingCount || 0
            })
        }).catch(() => {
            this.setData({ reports: [] })
        }).finally(() => {
            this.setData({ loading: false })
        })
    },

    // 切换筛选
    switchFilter(e) {
        const filter = e.currentTarget.dataset.filter
        this.setData({ activeFilter: filter, pageNum: 1 })
        this.loadReports()
    },

    // 搜索
    onSearchInput(e) {
        this.setData({ keyword: e.detail.value })
    },

    doSearch() {
        this.setData({ pageNum: 1 })
        this.loadReports()
    },

    // 查看详情
    viewDetail(e) {
        const id = e.currentTarget.dataset.id
        const report = this.data.reports.find(r => r.id === id)
        if (!report) return

        let content = `老人：${report.residentName}\n`
        if (report.roomBedNo) content += `床位：${report.roomBedNo}\n`
        if (report.careZone) content += `护理区：${report.careZone}\n`
        content += `症状：${report.symptomName}\n`
        content += `严重程度：${report.severityName}\n`
        content += `描述：${report.description}\n`
        if (report.location) content += `部位：${report.location}\n`
        if (report.startTime) content += `开始时间：${report.startTime}\n`
        if (report.triggers) content += `诱发因素：${report.triggers}\n`
        content += `状态：${report.statusText}`
        if (report.handleResult) {
            content += `\n处理回复：${report.handleResult}`
            content += `\n处理人：${report.handlerName}`
        }

        wx.showModal({
            title: '上报详情',
            content: content,
            showCancel: false
        })
    },

    // 护理员处理上报
    handleReport(e) {
        const id = e.currentTarget.dataset.id

        wx.showModal({
            title: '处理症状上报',
            editable: true,
            placeholderText: '请输入处理结果或回复...',
            success: (res) => {
                if (res.confirm && res.content) {
                    app.request(`/symptom/${id}/handle`, 'PUT', {
                        handlerId: this.data.userInfo.id,
                        handlerName: this.data.userInfo.realName || this.data.userInfo.username,
                        handleResult: res.content
                    }).then(() => {
                        wx.showToast({ title: '处理成功', icon: 'success' })
                        this.loadReports()
                    }).catch(err => {
                        wx.showToast({ title: err.message || '处理失败', icon: 'none' })
                    })
                }
            }
        })
    },

    onPullDownRefresh() {
        this.loadReports()
        setTimeout(() => { wx.stopPullDownRefresh() }, 1000)
    }
})
