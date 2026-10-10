// 护理计划页面
const app = getApp()

Page({
  data: {
    plans: [],
    filteredPlans: [],
    loading: false,
    refreshing: false,
    filterStatus: 'ALL', // ALL, ACTIVE, COMPLETED, OVERDUE
    selectedPlan: null,
    showPlanDetail: false,
    stats: {
      total: 0,
      active: 0,
      completed: 0,
      overdue: 0
    }
  },

  onLoad() {
    this.loadPlans()
  },

  onShow() {
    this.loadPlans()
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({ refreshing: true })
    
    // 模拟刷新延迟
    setTimeout(() => {
      this.setData({ 
        plans: this.getMockPlans(),
        stats: this.getMockStats(),
        refreshing: false
      })
      wx.stopPullDownRefresh()
    }, 1000)
  },

  // 加载护理计划
  loadPlans() {
    this.setData({ loading: true })
    
    const userInfo = wx.getStorageSync('userInfo')
    if (!userInfo) {
      this.setData({ loading: false })
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    wx.request({
      url: getApp().globalData.baseUrl + '/care-plan/list',
      method: 'GET',
      data: {
        current: 1,
        size: 100,
        userId: userInfo.id,
        role: userInfo.role
      },
      success: (res) => {
        if (res.data.code === 200) {
          const plans = res.data.data.records || []
          
          // 为每个计划添加任务统计（如果后端没有返回）
          plans.forEach(plan => {
            if (!plan.completedTaskCount) {
              plan.completedTaskCount = 0
              plan.totalTaskCount = 0
            }
          })
          
          // 计算统计数据
          const stats = {
            total: plans.length,
            active: plans.filter(p => p.status === 'ACTIVE').length,
            completed: plans.filter(p => p.status === 'COMPLETED').length,
            overdue: plans.filter(p => p.status === 'OVERDUE').length
          }
          
          this.setData({ 
            plans: plans,
            filteredPlans: plans,
            stats: stats,
            loading: false
          })
        } else {
          this.setData({ loading: false })
          wx.showToast({ title: res.data.message || '加载失败', icon: 'none' })
        }
      },
      fail: () => {
        this.setData({ loading: false })
        wx.showToast({ title: '网络错误', icon: 'none' })
      }
    })
  },

  // 获取模拟护理计划数据
  getMockPlans() {
    const plans = [
      {
        id: 1,
        residentName: '张爷爷',
        bedNo: '101-1',
        careZone: '自理区',
        planTitle: '跌倒风险防护计划',
        planType: 'FALL_PREVENTION',
        status: 'ACTIVE',
        priority: 'HIGH',
        startDate: '2026-02-01',
        endDate: '2026-02-07',
        progress: 60,
        assignedCaregiver: '李护理员',
        tasks: [
          { id: 1, title: '每2小时巡视一次', completed: true, time: '08:00' },
          { id: 2, title: '协助老人如厕', completed: true, time: '10:00' },
          { id: 3, title: '检查防滑鞋佩戴情况', completed: false, time: '14:00' },
          { id: 4, title: '床栏安全检查', completed: false, time: '16:00' }
        ],
        notes: '老人配合度良好，已告知家属注意事项'
      },
      {
        id: 2,
        residentName: '李奶奶',
        bedNo: '102-1',
        careZone: '全护区',
        planTitle: '压疮预防护理计划',
        planType: 'PRESSURE_PREVENTION',
        status: 'ACTIVE',
        priority: 'HIGH',
        startDate: '2026-02-01',
        endDate: '2026-02-10',
        progress: 80,
        assignedCaregiver: '王护理员',
        tasks: [
          { id: 1, title: '2小时翻身一次', completed: true, time: '08:00' },
          { id: 2, title: '皮肤状态评估', completed: true, time: '12:00' },
          { id: 3, title: '更换气垫床垫', completed: true, time: '14:00' },
          { id: 4, title: '营养状态评估', completed: false, time: '18:00' }
        ],
        notes: '皮肤状态良好，无红肿破损'
      },
      {
        id: 3,
        residentName: '王爷爷',
        bedNo: '201-1',
        careZone: '半护区',
        planTitle: '日常康复活动计划',
        planType: 'REHABILITATION',
        status: 'COMPLETED',
        priority: 'MEDIUM',
        startDate: '2026-01-25',
        endDate: '2026-02-01',
        progress: 100,
        assignedCaregiver: '赵护理员',
        tasks: [
          { id: 1, title: '晨间关节活动', completed: true, time: '09:00' },
          { id: 2, title: '步行训练指导', completed: true, time: '15:00' },
          { id: 3, title: '活动耐力观察', completed: true, time: '20:00' }
        ],
        notes: '康复效果良好，老人已离院'
      },
      {
        id: 4,
        residentName: '赵奶奶',
        bedNo: '203-1',
        careZone: '失智照护区',
        planTitle: '认知陪伴照护计划',
        planType: 'COGNITIVE_CARE',
        status: 'OVERDUE',
        priority: 'MEDIUM',
        startDate: '2026-01-30',
        endDate: '2026-02-02',
        progress: 40,
        assignedCaregiver: '钱照护员',
        tasks: [
          { id: 1, title: '记忆卡片活动', completed: true, time: '10:00' },
          { id: 2, title: '熟悉环境陪伴', completed: false, time: '14:00' },
          { id: 3, title: '情绪与行为观察', completed: false, time: '16:00' }
        ],
        notes: '老人依从性较差，需要加强宣教'
      }
    ]
    
    // 为每个计划添加预计算的任务统计字段
    return plans.map(plan => {
      const completedTaskCount = plan.tasks.filter(task => task.completed).length
      const totalTaskCount = plan.tasks.length
      
      return {
        ...plan,
        completedTaskCount,
        totalTaskCount
      }
    })
  },

  // 获取模拟统计数据
  getMockStats() {
    return {
      total: 12,
      active: 6,
      completed: 4,
      overdue: 2
    }
  },

  // 筛选护理计划
  filterPlans(e) {
    const filterStatus = e.currentTarget.dataset.status
    const { plans } = this.data
    
    let filteredPlans = plans
    if (filterStatus !== 'ALL') {
      filteredPlans = plans.filter(plan => plan.status === filterStatus)
    }
    
    this.setData({ 
      filterStatus,
      filteredPlans
    })
  },

  // 获取筛选后的计划列表
  getFilteredPlans() {
    const { plans, filterStatus } = this.data
    if (filterStatus === 'ALL') return plans
    
    return plans.filter(plan => plan.status === filterStatus)
  },

  // 查看计划详情
  viewPlanDetail(e) {
    const planId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/care/plan-detail?id=${planId}`
    })
  },

  // 关闭计划详情
  closePlanDetail() {
    this.setData({ 
      showPlanDetail: false,
      selectedPlan: null 
    })
  },

  // 完成任务
  completeTask(e) {
    const { taskId } = e.currentTarget.dataset
    const userInfo = wx.getStorageSync('userInfo')
    
    if (!userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    wx.request({
      url: getApp().globalData.baseUrl + `/care-plan/task/${taskId}/complete`,
      method: 'PUT',
      data: {
        userId: userInfo.id,
        userName: userInfo.name
      },
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({ title: '任务已完成', icon: 'success' })
          this.loadPlans()
        } else {
          wx.showToast({ title: res.data.message || '操作失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' })
      }
    })
  },

  // 添加护理记录
  addNursingRecord(e) {
    const planId = e.currentTarget.dataset.id
    
    wx.showModal({
      title: '添加护理记录',
      content: '请输入护理记录内容',
      editable: true,
      placeholderText: '请输入护理记录',
      success: (res) => {
        if (res.confirm && res.content) {
          const plans = this.data.plans.map(plan => {
            if (plan.id === planId) {
              plan.notes = res.content
            }
            return plan
          })
          
          this.setData({ plans })
          wx.showToast({
            title: '记录添加成功',
            icon: 'success'
          })
        }
      }
    })
  },

  // 创建新计划
  createNewPlan() {
    wx.navigateTo({
      url: '/pages/care/create-plan'
    })
  },

  // 获取计划类型文本
  getPlanTypeText(type) {
    const typeMap = {
      'FALL_PREVENTION': '跌倒防护',
      'PRESSURE_PREVENTION': '压疮预防',
      'REHABILITATION': '康复护理',
      'COGNITIVE_CARE': '认知照护',
      'MEDICATION': '用药管理',
      'NUTRITION': '营养支持'
    }
    return typeMap[type] || type
  },

  // 获取优先级颜色
  getPriorityColor(priority) {
    const colorMap = {
      'HIGH': '#dc3545',
      'MEDIUM': '#ffc107',
      'LOW': '#28a745'
    }
    return colorMap[priority] || '#6c757d'
  },

  // 跳转到老人详情
  goToResidentDetail(e) {
    const planId = e.currentTarget.dataset.id
    const plan = this.data.plans.find(item => item.id === planId)
    
    if (plan) {
      wx.navigateTo({
        url: `/pages/resident/detail/detail?residentName=${plan.residentName}&bedNo=${plan.bedNo}`
      })
    }
  }
})
