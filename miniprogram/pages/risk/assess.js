const app = getApp()

Page({
  data: {
    residentId: '',
    residentName: '',
    scaleType: 'FALL',
    scaleList: [], // 可用量表列表
    questions: [],
    answers: {}, // 存储答题结果 {questionId: selectedValue}
    totalScore: 0,
    loading: false,
    offlineMode: false // 离线模式标识
  },
  
  onLoad(opt) {
    if (opt.residentId) {
      this.setData({ 
        residentId: opt.residentId,
        residentName: opt.residentName || ''
      })
    }
    this.loadScaleList()
  },

  onShow() {
    // 页面显示时检查是否有草稿
    if (this.data.residentId && this.data.scaleType) {
      this.loadDraft()
    }
  },
  
  // 加载可用量表列表
  loadScaleList() {
    wx.showLoading({ title: '加载中...' })
    
    app.request('/scale/enabled', 'GET').then(res => {
      this.setData({ scaleList: res || [] })
      if (res && res.length > 0) {
        this.setData({ scaleType: res[0].scaleType })
        this.loadScaleQuestions(res[0].scaleType)
      }
    }).catch(err => {
      console.error('加载量表列表失败:', err)
      // 离线模式：使用本地缓存的量表
      this.loadOfflineScale()
    }).finally(() => {
      wx.hideLoading()
    })
  },
  
  // 加载量表题目
  loadScaleQuestions(scaleType) {
    wx.showLoading({ title: '加载题目...' })
    
    app.request(`/scale/type/${scaleType}`, 'GET').then(res => {
      if (res && res.questions) {
        const questions = res.questions.map(q => ({
          id: q.id,
          questionOrder: q.questionOrder,
          questionText: q.questionText,
          questionType: q.questionType,
          required: q.required,
          options: JSON.parse(q.optionsJson || '[]')
        }))
        
        this.setData({ 
          questions,
          answers: {},
          totalScore: 0
        })
        
        // 缓存量表数据用于离线使用
        wx.setStorageSync(`scale_${scaleType}`, res)
      }
    }).catch(err => {
      console.error('加载题目失败:', err)
      wx.showToast({ title: '加载失败，请重试', icon: 'none' })
    }).finally(() => {
      wx.hideLoading()
    })
  },
  
  // 离线模式：加载本地缓存的量表
  loadOfflineScale() {
    const cachedScale = wx.getStorageSync(`scale_${this.data.scaleType}`)
    if (cachedScale && cachedScale.questions) {
      const questions = cachedScale.questions.map(q => ({
        id: q.id,
        questionOrder: q.questionOrder,
        questionText: q.questionText,
        questionType: q.questionType,
        required: q.required,
        options: JSON.parse(q.optionsJson || '[]')
      }))
      
      this.setData({ 
        questions,
        offlineMode: true
      })
      
      wx.showToast({ title: '离线模式', icon: 'none' })
    } else {
      wx.showToast({ title: '无可用量表', icon: 'none' })
    }
  },
  
  // 切换量表类型
  onScaleChange(e) {
    const scaleType = e.detail.value
    this.setData({ scaleType })
    this.loadScaleQuestions(scaleType)
  },
  
  // 答题变化
  onAnswerChange(e) {
    const questionId = e.currentTarget.dataset.questionId
    const value = e.detail.value
    
    const answers = { ...this.data.answers }
    answers[questionId] = value
    
    this.setData({ answers })
    this.calculateScore()
  },
  
  // 计算总分
  calculateScore() {
    let totalScore = 0
    const { questions, answers } = this.data
    
    questions.forEach(question => {
      const answer = answers[question.id]
      if (answer !== undefined) {
        const option = question.options.find(opt => opt.value == answer)
        if (option) {
          totalScore += option.score || 0
        }
      }
    })
    
    this.setData({ totalScore })
  },
  
  // 提交评估
  submit() {
    if (!this.data.residentId) {
      return wx.showToast({ title: '未指定老人', icon: 'none' })
    }
    
    // 验证必填题
    const { questions, answers } = this.data
    for (let question of questions) {
      if (question.required === 1 && !answers[question.id]) {
        return wx.showToast({ 
          title: `请回答：${question.questionText}`, 
          icon: 'none' 
        })
      }
    }
    
    const submitData = {
      residentId: this.data.residentId,
      evaluatorId: app.globalData.userInfo?.id || 1,
      evaluatorName: app.globalData.userInfo?.realName || '护理员',
      scaleType: this.data.scaleType,
      answers: this.data.answers,
      requestSource: 'MINIPROGRAM', // 需求11: 标记评估来源为小程序
      assessmentStatus: 'COMPLETED' // 需求12: 标记为已完成，从待评估列表移除
    }
    
    // 离线模式：保存到本地
    if (this.data.offlineMode || !wx.getNetworkType) {
      this.saveOfflineRecord(submitData)
      return
    }
    
    // 在线提交
    this.setData({ loading: true })
    
    app.request('/risk/submit', 'POST', submitData).then(res => {
      // 需求12: 清除草稿
      this.clearDraft()
      
      wx.showModal({
        title: '评估完成',
        content: `总分：${res.totalScore}\n风险等级：${this.getRiskLevelText(res.riskLevel)}\n\n建议措施：\n${res.interventions.join('\n')}\n\n老人已从待评估列表移除`,
        showCancel: false,
        success: () => {
          wx.navigateBack()
        }
      })
    }).catch(err => {
      console.error('提交失败:', err)
      // 提交失败，保存到本地
      wx.showModal({
        title: '提交失败',
        content: '是否保存到本地待网络恢复后上传？',
        success: (modalRes) => {
          if (modalRes.confirm) {
            this.saveOfflineRecord(submitData)
          }
        }
      })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },
  
  // 保存草稿
  saveDraft() {
    if (!this.data.residentId) return
    
    const draftData = {
      residentId: this.data.residentId,
      scaleType: this.data.scaleType,
      answers: this.data.answers,
      totalScore: this.data.totalScore,
      timestamp: Date.now()
    }
    
    wx.setStorageSync(`draft_${this.data.residentId}_${this.data.scaleType}`, draftData)
    wx.showToast({ title: '草稿已保存', icon: 'success' })
  },

  // 加载草稿
  loadDraft() {
    if (!this.data.residentId) return
    
    const draftKey = `draft_${this.data.residentId}_${this.data.scaleType}`
    const draft = wx.getStorageSync(draftKey)
    
    if (draft && draft.answers) {
      wx.showModal({
        title: '发现草稿',
        content: `发现${new Date(draft.timestamp).toLocaleString()}的草稿，是否恢复？`,
        success: (res) => {
          if (res.confirm) {
            this.setData({ 
              answers: draft.answers,
              totalScore: draft.totalScore || 0
            })
            this.calculateScore()
            wx.showToast({ title: '草稿已恢复', icon: 'success' })
          }
        }
      })
    }
  },

  // 清除草稿
  clearDraft() {
    if (!this.data.residentId) return
    
    const draftKey = `draft_${this.data.residentId}_${this.data.scaleType}`
    wx.removeStorageSync(draftKey)
  },

  // 保存离线记录
  saveOfflineRecord(data) {
    const offlineRecords = wx.getStorageSync('offline_records') || []
    offlineRecords.push({
      ...data,
      timestamp: Date.now(),
      synced: false
    })
    wx.setStorageSync('offline_records', offlineRecords)
    
    // 清除草稿
    this.clearDraft()
    
    wx.showToast({ 
      title: '已保存到本地', 
      icon: 'success',
      success: () => {
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      }
    })
  },

  // 获取风险等级文本
  getRiskLevelText(level) {
    const levelMap = {
      'HIGH': '高危',
      'MEDIUM': '中危',
      'LOW': '低危',
      'NONE': '无风险'
    }
    return levelMap[level] || level
  }
})
