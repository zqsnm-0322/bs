Page({
  data: {
    formData: {
      residentId: '',
      residentName: '',
      bedNo: '',
      careZone: '',
      planTitle: '',
      planType: '',
      planTypeLabel: '',
      priority: '',
      priorityLabel: '',
      startDate: '',
      endDate: '',
      assignedCaregiverId: '',
      assignedCaregiver: '',
      tasks: [],
      notes: ''
    },
    residents: [],
    caregivers: [],
    planTypes: [
      { value: 'FALL_PREVENTION', label: '跌倒防护' },
      { value: 'PRESSURE_PREVENTION', label: '压疮预防' },
      { value: 'REHABILITATION', label: '康复护理' },
      { value: 'RESPIRATORY', label: '呼吸训练' },
      { value: 'MEDICATION', label: '用药管理' },
      { value: 'NUTRITION', label: '营养支持' },
      { value: 'WOUND_CARE', label: '伤口护理' },
      { value: 'PAIN_MANAGEMENT', label: '疼痛管理' }
    ],
    priorities: [
      { value: 'HIGH', label: '高优先级' },
      { value: 'MEDIUM', label: '中优先级' },
      { value: 'LOW', label: '低优先级' }
    ],
    residentIndex: -1,
    caregiverIndex: -1,
    planTypeIndex: -1,
    priorityIndex: -1,
    userInfo: null,
    isCareManager: false
  },

  onLoad() {
    // 获取用户信息
    const userInfo = wx.getStorageSync('userInfo')
    const isCareManager = userInfo && userInfo.role === 'CARE_MANAGER'
    
    this.setData({
      userInfo: userInfo,
      isCareManager: isCareManager
    })

    // 设置默认日期
    const today = new Date()
    const startDate = this.formatDate(today)
    
    const endDate = new Date(today)
    endDate.setDate(endDate.getDate() + 7)
    const endDateStr = this.formatDate(endDate)
    
    this.setData({
      'formData.startDate': startDate,
      'formData.endDate': endDateStr
    })

    // 加载老人列表
    this.loadResidents()

    // 如果是护理员，设置默认负责人为自己
    if (!isCareManager && userInfo) {
      this.setData({
        'formData.assignedCaregiverId': userInfo.id,
        'formData.assignedCaregiver': userInfo.realName || userInfo.username
      })
    }
  },

  loadResidents() {
    wx.request({
      url: getApp().globalData.baseUrl + '/care-plan/residents',
      method: 'GET',
      success: (res) => {
        if (res.data.code === 200) {
          const residents = res.data.data.map(p => ({
            id: p.id,
            name: p.name,
            bedNo: p.bedNo,
            careZone: p.careZone,
            displayName: `${p.name} - ${p.bedNo}床 - ${p.careZone}`
          }))
          this.setData({ residents })
        }
      }
    })
  },

  loadCaregivers(careZone) {
    wx.request({
      url: getApp().globalData.baseUrl + '/care-plan/caregivers',
      method: 'GET',
      data: { careZone: careZone },
      success: (res) => {
        if (res.data.code === 200) {
          const caregivers = res.data.data.map(n => ({
            id: n.id,
            name: n.name,
            careZone: n.careZone,
            displayName: `${n.name} - ${n.careZone}`
          }))
          this.setData({ caregivers })
        }
      }
    })
  },

  formatDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    this.setData({
      [`formData.${field}`]: value
    })
  },

  onResidentChange(e) {
    const index = e.detail.value
    const resident = this.data.residents[index]
    
    if (!resident) {
      console.warn('老人信息不存在，索引:', index)
      return
    }
    
    this.setData({
      residentIndex: index,
      'formData.residentId': resident.id,
      'formData.residentName': resident.name,
      'formData.bedNo': resident.bedNo,
      'formData.careZone': resident.careZone
    })

    // 如果是照护主管，加载该护理区的护理员列表
    if (this.data.isCareManager) {
      this.loadCaregivers(resident.careZone)
    }
  },

  onCaregiverChange(e) {
    const index = e.detail.value
    const caregiver = this.data.caregivers[index]
    this.setData({
      caregiverIndex: index,
      'formData.assignedCaregiverId': caregiver.id,
      'formData.assignedCaregiver': caregiver.name
    })
  },

  onPlanTypeChange(e) {
    const index = e.detail.value
    const planType = this.data.planTypes[index]
    this.setData({
      planTypeIndex: index,
      'formData.planType': planType.value,
      'formData.planTypeLabel': planType.label
    })
  },

  onPriorityChange(e) {
    const index = e.detail.value
    const priority = this.data.priorities[index]
    this.setData({
      priorityIndex: index,
      'formData.priority': priority.value,
      'formData.priorityLabel': priority.label
    })
  },

  onStartDateChange(e) {
    this.setData({
      'formData.startDate': e.detail.value
    })
  },

  onEndDateChange(e) {
    this.setData({
      'formData.endDate': e.detail.value
    })
  },

  addTask() {
    const tasks = [...this.data.formData.tasks]
    tasks.push({
      title: '',
      time: '08:00',
      completed: false
    })
    this.setData({
      'formData.tasks': tasks
    })
  },

  removeTask(e) {
    const index = e.currentTarget.dataset.index
    const tasks = [...this.data.formData.tasks]
    tasks.splice(index, 1)
    this.setData({
      'formData.tasks': tasks
    })
  },

  onTaskTitleChange(e) {
    const index = e.currentTarget.dataset.index
    const value = e.detail.value
    const tasks = [...this.data.formData.tasks]
    tasks[index].title = value
    this.setData({
      'formData.tasks': tasks
    })
  },

  onTaskTimeChange(e) {
    const index = e.currentTarget.dataset.index
    const value = e.detail.value
    const tasks = [...this.data.formData.tasks]
    tasks[index].time = value
    this.setData({
      'formData.tasks': tasks
    })
  },

  validateForm() {
    const { residentId, residentName, planTitle, planType, priority, startDate, endDate, assignedCaregiver, tasks } = this.data.formData
    
    if (!residentId) {
      wx.showToast({ title: '请选择老人', icon: 'none' })
      return false
    }
    
    if (!planTitle || !planTitle.trim()) {
      wx.showToast({ title: '请输入计划标题', icon: 'none' })
      return false
    }
    
    if (!planType) {
      wx.showToast({ title: '请选择计划类型', icon: 'none' })
      return false
    }
    
    if (!priority) {
      wx.showToast({ title: '请选择优先级', icon: 'none' })
      return false
    }
    
    if (!startDate) {
      wx.showToast({ title: '请选择开始日期', icon: 'none' })
      return false
    }
    
    if (!endDate) {
      wx.showToast({ title: '请选择结束日期', icon: 'none' })
      return false
    }
    
    if (new Date(endDate) < new Date(startDate)) {
      wx.showToast({ title: '结束日期不能早于开始日期', icon: 'none' })
      return false
    }
    
    // 照护主管必须指定护理员
    if (this.data.isCareManager && !assignedCaregiver) {
      wx.showToast({ title: '请选择负责护理员', icon: 'none' })
      return false
    }
    
    if (tasks.length === 0) {
      wx.showToast({ title: '请至少添加一个护理任务', icon: 'none' })
      return false
    }
    
    // 验证任务内容
    for (let i = 0; i < tasks.length; i++) {
      if (!tasks[i].title || !tasks[i].title.trim()) {
        wx.showToast({ title: `请填写任务${i + 1}的内容`, icon: 'none' })
        return false
      }
    }
    
    return true
  },

  onSubmit() {
    if (!this.validateForm()) {
      return
    }

    wx.showLoading({ title: '创建中...', mask: true })

    const userInfo = this.data.userInfo
    
    // 构建提交数据
    const planData = {
      ...this.data.formData,
      creatorId: userInfo.id,
      creatorName: userInfo.realName || userInfo.username,
      creatorRole: userInfo.role,
      // 如果是护理员创建且未指定负责人，默认为自己
      assignedCaregiverId: this.data.formData.assignedCaregiverId || (userInfo.role === 'CAREGIVER' ? userInfo.id : null),
      assignedCaregiver: this.data.formData.assignedCaregiver || (userInfo.role === 'CAREGIVER' ? (userInfo.realName || userInfo.username) : '')
    }

    wx.request({
      url: getApp().globalData.baseUrl + '/care-plan',
      method: 'POST',
      data: planData,
      success: (res) => {
        wx.hideLoading()
        if (res.data.code === 200) {
          wx.showToast({
            title: '创建成功',
            icon: 'success',
            duration: 2000
          })

          setTimeout(() => {
            wx.navigateBack({
              success: () => {
                const pages = getCurrentPages()
                const prevPage = pages[pages.length - 2]
                if (prevPage && prevPage.loadPlans) {
                  prevPage.loadPlans()
                }
              }
            })
          }, 2000)
        } else {
          wx.showToast({
            title: res.data.message || '创建失败',
            icon: 'none'
          })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        })
      }
    })
  },

  onCancel() {
    wx.navigateBack()
  }
})
